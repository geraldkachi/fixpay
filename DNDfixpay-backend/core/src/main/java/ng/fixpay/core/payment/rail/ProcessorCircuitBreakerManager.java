package ng.fixpay.core.payment.rail;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Manages per-processor Resilience4j circuit breakers for transparent automatic failover.
 *
 * <h3>How failover works</h3>
 * <ol>
 *   <li>{@link PaymentRailRegistry#resolve} iterates candidates in priority order.</li>
 *   <li>For each candidate it calls {@link #isAvailable(String)} — skips any processor
 *       whose circuit is OPEN or FORCED_OPEN.</li>
 *   <li>After a successful adapter call, the caller invokes {@link #recordSuccess}.</li>
 *   <li>After a failed adapter call, the caller invokes {@link #recordFailure}.</li>
 *   <li>Once failure rate exceeds the threshold, the circuit opens automatically and
 *       the registry falls through to the next configured processor.</li>
 *   <li>After {@code waitDurationSeconds}, the circuit transitions to HALF_OPEN and
 *       allows a probe call; if successful, it closes again automatically.</li>
 * </ol>
 *
 * <h3>Manual maintenance</h3>
 * <p>An admin can force a processor into maintenance (FORCED_OPEN) via the admin API.
 * The {@link ng.fixpay.core.payment.rail.domain.PaymentRailConfig#isMaintenance()} flag
 * on the DB row serves as the durable record; {@link #forceOpen}/{@link #forceClose}
 * align the in-memory CB state accordingly.
 *
 * <h3>Per-processor overrides</h3>
 * <p>Circuit breaker parameters can be overridden per-processor by including the
 * following keys in the {@code config_json} column:
 * <ul>
 *   <li>{@code cbFailureRateThreshold} — 0–100 (default 50)</li>
 *   <li>{@code cbSlidingWindowSize} — (default 10)</li>
 *   <li>{@code cbWaitDurationSeconds} — (default 60)</li>
 *   <li>{@code cbMinCalls} — (default 5)</li>
 * </ul>
 */
@Component
public class ProcessorCircuitBreakerManager {

    private static final Logger log = LoggerFactory.getLogger(ProcessorCircuitBreakerManager.class);

    private final CircuitBreakerRegistry cbRegistry;
    private final CircuitBreakerConfig   defaultConfig;
    private final Map<String, CircuitBreaker> circuitBreakers = new ConcurrentHashMap<>();

    // Default values stored for use in applyOverrides fallback
    private final float defaultFailureRateThreshold;
    private final int   defaultSlidingWindowSize;
    private final long  defaultWaitDurationSeconds;
    private final int   defaultMinCalls;

    public ProcessorCircuitBreakerManager(
            @Value("${fixpay.rail.cb.failure-rate-threshold:50}") float failureRateThreshold,
            @Value("${fixpay.rail.cb.sliding-window-size:10}")    int   slidingWindowSize,
            @Value("${fixpay.rail.cb.wait-duration-seconds:60}")  long  waitDurationSeconds,
            @Value("${fixpay.rail.cb.min-calls:5}")               int   minimumNumberOfCalls
    ) {
        this.defaultFailureRateThreshold = failureRateThreshold;
        this.defaultSlidingWindowSize    = slidingWindowSize;
        this.defaultWaitDurationSeconds  = waitDurationSeconds;
        this.defaultMinCalls             = minimumNumberOfCalls;
        this.defaultConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRateThreshold)
                .slidingWindowSize(slidingWindowSize)
                .waitDurationInOpenState(Duration.ofSeconds(waitDurationSeconds))
                .minimumNumberOfCalls(minimumNumberOfCalls)
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();
        this.cbRegistry = CircuitBreakerRegistry.of(defaultConfig);
    }

    // ─── Core API ─────────────────────────────────────────────────────────────

    /**
     * Returns {@code true} when the circuit is CLOSED or HALF_OPEN (processor may be tried).
     * Returns {@code false} when OPEN or FORCED_OPEN (skip this processor).
     */
    public boolean isAvailable(String processorId) {
        CircuitBreaker.State state = getCircuitBreaker(processorId).getState();
        return state == CircuitBreaker.State.CLOSED || state == CircuitBreaker.State.HALF_OPEN;
    }

    /** Records a successful adapter call. Helps close a HALF_OPEN circuit. */
    public void recordSuccess(String processorId) {
        getCircuitBreaker(processorId).onSuccess(0, TimeUnit.MILLISECONDS);
    }

    /** Records a failed adapter call. May open the circuit after threshold breach. */
    public void recordFailure(String processorId, Throwable throwable) {
        getCircuitBreaker(processorId).onError(0, TimeUnit.MILLISECONDS, throwable);
    }

    /** Returns the current circuit breaker state for a processor. */
    public CircuitBreaker.State getState(String processorId) {
        return getCircuitBreaker(processorId).getState();
    }

    /** Returns failure rate (0–100) over the sliding window. Returns -1 if not enough calls. */
    public float getFailureRate(String processorId) {
        return getCircuitBreaker(processorId).getMetrics().getFailureRate();
    }

    /** Returns total number of calls recorded in the current sliding window. */
    public long getTotalCalls(String processorId) {
        CircuitBreaker.Metrics metrics = getCircuitBreaker(processorId).getMetrics();
        return metrics.getNumberOfSuccessfulCalls() + metrics.getNumberOfFailedCalls();
    }

    // ─── Manual circuit control (admin API) ──────────────────────────────────

    /** Forces the circuit to FORCED_OPEN — processor will be skipped until closed. */
    public void forceOpen(String processorId) {
        getCircuitBreaker(processorId).transitionToForcedOpenState();
        log.info("[ProcessorCircuitBreakerManager] Circuit FORCE-OPENED for '{}'", processorId);
    }

    /** Closes a manually forced-open circuit, allowing the processor back into rotation. */
    public void forceClose(String processorId) {
        getCircuitBreaker(processorId).transitionToClosedState();
        log.info("[ProcessorCircuitBreakerManager] Circuit FORCE-CLOSED for '{}'", processorId);
    }

    // ─── Per-processor configuration ──────────────────────────────────────────

    /**
     * Applies per-processor circuit breaker overrides from the {@code config_json} map.
     * Called by the registry whenever a config row is resolved.
     */
    public void applyOverrides(String processorId, Map<String, String> processorConfig) {
        if (processorConfig == null || processorConfig.isEmpty()) return;
        boolean hasOverride = processorConfig.containsKey("cbFailureRateThreshold")
                || processorConfig.containsKey("cbSlidingWindowSize")
                || processorConfig.containsKey("cbWaitDurationSeconds")
                || processorConfig.containsKey("cbMinCalls");
        if (!hasOverride) return;

        float failureRate = parseFloat(processorConfig, "cbFailureRateThreshold",
                defaultFailureRateThreshold);
        int   window      = parseInt  (processorConfig, "cbSlidingWindowSize",
                defaultSlidingWindowSize);
        long  waitSecs    = parseLong (processorConfig, "cbWaitDurationSeconds",
                defaultWaitDurationSeconds);
        int   minCalls    = parseInt  (processorConfig, "cbMinCalls",
                defaultMinCalls);

        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(failureRate)
                .slidingWindowSize(window)
                .waitDurationInOpenState(Duration.ofSeconds(waitSecs))
                .minimumNumberOfCalls(minCalls)
                .permittedNumberOfCallsInHalfOpenState(2)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .build();

        CircuitBreaker cb = cbRegistry.circuitBreaker(processorId, config);
        circuitBreakers.put(processorId, cb);
        log.debug("[ProcessorCircuitBreakerManager] Applied CB overrides for '{}': " +
                  "failRate={}%, window={}, waitSecs={}, minCalls={}",
                  processorId, failureRate, window, waitSecs, minCalls);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private CircuitBreaker getCircuitBreaker(String processorId) {
        return circuitBreakers.computeIfAbsent(processorId,
                id -> cbRegistry.circuitBreaker(id, defaultConfig));
    }

    private float parseFloat(Map<String, String> map, String key, float def) {
        try { return Float.parseFloat(map.get(key)); } catch (Exception e) { return def; }
    }

    private int parseInt(Map<String, String> map, String key, int def) {
        try { return Integer.parseInt(map.get(key)); } catch (Exception e) { return def; }
    }

    private long parseLong(Map<String, String> map, String key, long def) {
        try { return Long.parseLong(map.get(key)); } catch (Exception e) { return def; }
    }
}
