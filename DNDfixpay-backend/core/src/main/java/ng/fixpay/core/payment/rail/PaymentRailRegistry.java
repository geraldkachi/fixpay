package ng.fixpay.core.payment.rail;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfig;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfigRepository;
import ng.fixpay.shared.exception.FixPayException;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailExtensionPoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Resolves the active {@link PaymentRailAdapter} for a given tenant and payment method,
 * incorporating circuit breaker state, maintenance flags, and hot-loadable PF4J plugins.
 *
 * <h3>Resolution order</h3>
 * <ol>
 *   <li>Query {@code payment_rail_config} for enabled rows matching
 *       {@code (tenantId, paymentMethod)}, ordered by tenant-specific rows first, then
 *       global defaults; within each group by {@code priority ASC}.</li>
 *   <li>Walk the result list. For each row:
 *     <ul>
 *       <li>Skip if {@code config.isMaintenance()} is true.</li>
 *       <li>Skip if the circuit breaker for this processorId is OPEN / FORCED_OPEN.</li>
 *       <li>Otherwise: find the adapter (Spring bean or PF4J plugin) and return it.</li>
 *     </ul>
 *   </li>
 *   <li>If no match is found, throw {@link FixPayException#serviceUnavailable} (HTTP 503).</li>
 * </ol>
 *
 * <h3>Automatic failover</h3>
 * <p>Callers must call {@link #recordSuccess} / {@link #recordFailure} after each
 * adapter invocation so circuit breakers self-manage.
 *
 * <h3>PF4J hot-loaded plugins</h3>
 * <p>At startup, PF4J extensions are merged into the adapter map. Spring bean
 * adapters take precedence over plugins with the same processorId. Plugins can be
 * hot-loaded / unloaded at runtime via the admin API; call
 * {@link #refreshPluginAdapters()} after a plugin change.
 */
@Component
public class PaymentRailRegistry {

    private static final Logger log = LoggerFactory.getLogger(PaymentRailRegistry.class);

    private final PaymentRailConfigRepository      configRepository;
    private final ProcessorCircuitBreakerManager   cbManager;
    private final ObjectMapper                     objectMapper;

    /** Mutable map so PF4J adapters can be added/removed at runtime. */
    private final ConcurrentHashMap<String, PaymentRailAdapter> adaptersByProcessorId
            = new ConcurrentHashMap<>();

    /** Snapshot of Spring-bean adapters only (never evicted at runtime). */
    private final Map<String, PaymentRailAdapter> springAdapters;

    @Nullable
    private final FixPayPluginManager pluginManager;

    public PaymentRailRegistry(PaymentRailConfigRepository configRepository,
                                List<PaymentRailAdapter> adapters,
                                ObjectMapper objectMapper,
                                ProcessorCircuitBreakerManager cbManager,
                                @Autowired(required = false) @Nullable FixPayPluginManager pluginManager) {
        this.configRepository = configRepository;
        this.objectMapper     = objectMapper;
        this.cbManager        = cbManager;
        this.pluginManager    = pluginManager;

        this.springAdapters = adapters.stream()
                .collect(Collectors.toMap(PaymentRailAdapter::processorId, Function.identity()));
        adaptersByProcessorId.putAll(springAdapters);

        log.info("[PaymentRailRegistry] Registered {} Spring adapter(s): {}",
                springAdapters.size(),
                springAdapters.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }

    // ─── Startup ──────────────────────────────────────────────────────────────

    /**
     * Merges PF4J plugin adapters into the registry after the application context is ready
     * (guarantees plugins are loaded AFTER Spring beans, so beans always win on ID clash).
     */
    @EventListener(ApplicationReadyEvent.class)
    public void mergePluginAdapters() {
        if (pluginManager == null) return;
        List<PaymentRailExtensionPoint> extensions = pluginManager.getExtensions();
        int added = 0;
        for (PaymentRailExtensionPoint ext : extensions) {
            if (!springAdapters.containsKey(ext.processorId())) {
                adaptersByProcessorId.put(ext.processorId(), ext);
                added++;
                log.info("[PaymentRailRegistry] Loaded plugin adapter '{}'", ext.processorId());
            } else {
                log.warn("[PaymentRailRegistry] Plugin adapter '{}' skipped — Spring bean takes precedence",
                        ext.processorId());
            }
        }
        if (added > 0) {
            log.info("[PaymentRailRegistry] {} plugin adapter(s) merged. Total adapters: {}",
                    added, adaptersByProcessorId.size());
        }
    }

    /**
     * Re-merges plugin adapters after a hot-load / unload operation.
     * Called by the admin API plugin management endpoint.
     */
    public void refreshPluginAdapters() {
        // Remove all plugin-only entries (keep Spring beans)
        adaptersByProcessorId.keySet().removeIf(id -> !springAdapters.containsKey(id));
        mergePluginAdapters();
        log.info("[PaymentRailRegistry] Plugin adapters refreshed. Active: {}",
                adaptersByProcessorId.keySet().stream().sorted().collect(Collectors.joining(", ")));
    }

    // ─── Resolution ───────────────────────────────────────────────────────────

    /**
     * Resolves the best available adapter with circuit-breaker and maintenance checks.
     *
     * @param tenantId      the tenant making the payment (null = global-only lookup)
     * @param paymentMethod the chosen payment method
     * @return the resolved adapter with its runtime config
     * @throws FixPayException (503) if no healthy, enabled adapter is available
     */
    public ResolvedAdapter resolve(UUID tenantId, VtpassPaymentMethod paymentMethod) {
        List<PaymentRailConfig> candidates =
                configRepository.findEnabledByTenantAndMethod(tenantId, paymentMethod);

        for (PaymentRailConfig config : candidates) {
            String processorId = config.getProcessorId();

            if (config.isMaintenance()) {
                log.debug("[PaymentRailRegistry] {} is in maintenance — skipping", processorId);
                continue;
            }

            Map<String, String> processorConfig = parseConfig(config.getConfigJson());
            cbManager.applyOverrides(processorId, processorConfig);

            if (!cbManager.isAvailable(processorId)) {
                log.warn("[PaymentRailRegistry] Circuit OPEN for '{}' — falling through to next candidate",
                        processorId);
                continue;
            }

            PaymentRailAdapter adapter = adaptersByProcessorId.get(processorId);
            if (adapter != null) {
                log.debug("[PaymentRailRegistry] Resolved {} → {} (tenant={})",
                        paymentMethod, processorId, tenantId);
                return new ResolvedAdapter(adapter, processorConfig, config.getId());
            }
            log.warn("[PaymentRailRegistry] Config row references unknown processorId '{}' for method {} — skipping",
                    processorId, paymentMethod);
        }

        throw FixPayException.serviceUnavailable(
                "No active payment processor is available for method '" + paymentMethod + "'. " +
                "All configured processors are in maintenance, circuit-open, or not installed. " +
                "Please contact support or try a different payment method.");
    }

    /**
     * Resolves an adapter directly by processorId without any circuit-breaker or
     * maintenance checks. Used in {@code execute()} to look up the exact processor
     * that was used during {@code initiate()}.
     *
     * @param processorId the processorId stored on the payment entity
     * @return optional adapter (empty if the processor plugin was unloaded)
     */
    public Optional<PaymentRailAdapter> resolveById(String processorId) {
        return Optional.ofNullable(adaptersByProcessorId.get(processorId));
    }

    // ─── Circuit breaker delegates ────────────────────────────────────────────

    public void recordSuccess(String processorId) {
        cbManager.recordSuccess(processorId);
    }

    public void recordFailure(String processorId, Throwable t) {
        cbManager.recordFailure(processorId, t);
    }

    // ─── Health summaries ─────────────────────────────────────────────────────

    /**
     * Returns a health summary for every known processorId (all registered adapters).
     * Used by the admin health dashboard endpoint.
     */
    public List<ProcessorHealthStatus> getProcessorHealthSummaries() {
        List<ProcessorHealthStatus> result = new ArrayList<>();
        for (String id : adaptersByProcessorId.keySet()) {
            CircuitBreaker.State state = cbManager.getState(id);
            float failureRate = cbManager.getFailureRate(id);
            long totalCalls = cbManager.getTotalCalls(id);
            boolean isPlugin = !springAdapters.containsKey(id);
            result.add(new ProcessorHealthStatus(id, state.name(), failureRate, totalCalls, isPlugin));
        }
        result.sort((a, b) -> a.processorId().compareTo(b.processorId()));
        return result;
    }

    /** Returns all registered adapter processor IDs (for admin discovery). */
    public List<String> registeredProcessorIds() {
        return List.copyOf(adaptersByProcessorId.keySet());
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private Map<String, String> parseConfig(String configJson) {
        if (configJson == null || configJson.isBlank() || "{}".equals(configJson.trim())) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(configJson, new TypeReference<>() {});
        } catch (Exception ex) {
            log.warn("[PaymentRailRegistry] Could not parse config_json, using empty config: {}", ex.getMessage());
            return Collections.emptyMap();
        }
    }

    // ─── Value objects ────────────────────────────────────────────────────────

    /**
     * A resolved adapter with its runtime config and the DB config row ID.
     * {@code railConfigId} is used by {@link FeeCalculatorService} to look up the
     * active fee schedule.
     */
    public record ResolvedAdapter(
            PaymentRailAdapter adapter,
            Map<String, String> processorConfig,
            UUID railConfigId
    ) {}

    /**
     * Live health status of a processor for the admin dashboard.
     *
     * @param processorId  the processor identifier
     * @param cbState      Resilience4j circuit breaker state name (CLOSED / OPEN / HALF_OPEN / FORCED_OPEN)
     * @param failureRate  failure rate percentage over the sliding window (-1 if not enough data)
     * @param totalCalls   total calls recorded in the sliding window
     * @param isPlugin     true if this adapter was loaded via PF4J (not a Spring bean)
     */
    public record ProcessorHealthStatus(
            String processorId,
            String cbState,
            float  failureRate,
            long   totalCalls,
            boolean isPlugin
    ) {}
}
