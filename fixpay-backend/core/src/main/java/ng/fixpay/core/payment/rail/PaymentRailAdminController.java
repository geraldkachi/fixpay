package ng.fixpay.core.payment.rail;

import jakarta.servlet.http.HttpServletRequest;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.rail.domain.PaymentRailAuditLog;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfig;
import ng.fixpay.core.payment.rail.domain.ProcessorFeeSchedule;
import ng.fixpay.core.payment.rail.domain.ProcessorFeeScheduleRepository;
import ng.fixpay.core.payment.rail.dto.FeeScheduleRequest;
import ng.fixpay.core.payment.rail.dto.PaymentRailConfigRequest;
import ng.fixpay.core.payment.rail.dto.PaymentRailConfigResponse;
import ng.fixpay.shared.exception.FixPayException;
import ng.fixpay.shared.payment.ConfigSchema;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import org.pf4j.PluginWrapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST API for platform admins to manage the payment rail system.
 *
 * <p>All endpoints require the {@code PLATFORM_ADMIN} role (enforced by {@code SecurityConfig}).
 *
 * <h2>Endpoints</h2>
 * <pre>
 * Rail configuration
 *   GET    /api/admin/rails                — list all configs (optionally filtered by tenantId)
 *   GET    /api/admin/rails/{id}           — get config by ID (includes configSchema)
 *   POST   /api/admin/rails                — create a new config
 *   PUT    /api/admin/rails/{id}/config    — update configJson / priority
 *   PUT    /api/admin/rails/{id}/processor — change processorId
 *   PATCH  /api/admin/rails/{id}/enabled   — toggle enabled flag
 *   PATCH  /api/admin/rails/{id}/maintenance — toggle maintenance flag
 *   DELETE /api/admin/rails/{id}           — delete config row
 *
 * Discovery
 *   GET    /api/admin/rails/processors     — list all registered processor IDs
 *   GET    /api/admin/rails/processors/{processorId}/schema — get config schema for a processor
 *   GET    /api/admin/rails/health         — circuit breaker health summary per processor
 *
 * Fee schedule
 *   GET    /api/admin/rails/{id}/fees      — list fee schedules for a config
 *   POST   /api/admin/rails/{id}/fees      — add a fee schedule row
 *   DELETE /api/admin/rails/{id}/fees/{feeId} — delete a fee schedule row
 *
 * Plugin management (hot-loaded PF4J processors)
 *   GET    /api/admin/plugins              — list loaded plugins
 *   DELETE /api/admin/plugins/{pluginId}   — unload a plugin
 *   POST   /api/admin/plugins/reload       — reload all plugins from ./plugins dir
 *
 * Audit log
 *   GET    /api/admin/rails/audit          — paginated audit log (all)
 *   GET    /api/admin/rails/{id}/audit     — audit log for a specific config row
 *
 * Settlement
 *   GET    /api/admin/settlement/report?tenantId=&from=&to=  — settlement report (stub)
 * </pre>
 */
@RestController
@RequestMapping("/api/admin")
public class PaymentRailAdminController {

    private final PaymentRailConfigService      configService;
    private final PaymentRailRegistry           registry;
    private final RailAdminAuditService         auditService;
    private final ProcessorFeeScheduleRepository feeRepository;
    private final SettlementReportService       settlementService;
    private final FixPayPluginManager           pluginManager;   // may be null if plugins not configured

    public PaymentRailAdminController(PaymentRailConfigService configService,
                                       PaymentRailRegistry registry,
                                       RailAdminAuditService auditService,
                                       ProcessorFeeScheduleRepository feeRepository,
                                       SettlementReportService settlementService,
                                       Optional<FixPayPluginManager> pluginManager) {
        this.configService    = configService;
        this.registry         = registry;
        this.auditService     = auditService;
        this.feeRepository    = feeRepository;
        this.settlementService = settlementService;
        this.pluginManager    = pluginManager.orElse(null);
    }

    // ─── Rail configuration ───────────────────────────────────────────────────

    @GetMapping("/rails")
    public List<PaymentRailConfigResponse> listRails(
            @RequestParam(required = false) UUID tenantId,
            @AuthenticationPrincipal Jwt jwt) {
        return configService.listForTenant(tenantId).stream()
                .map(cfg -> PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId())))
                .collect(Collectors.toList());
    }

    @GetMapping("/rails/{id}")
    public PaymentRailConfigResponse getRail(@PathVariable UUID id) {
        PaymentRailConfig cfg = configService.getById(id);
        return PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId()));
    }

    @PostMapping("/rails")
    public ResponseEntity<PaymentRailConfigResponse> createRail(
            @RequestBody PaymentRailConfigRequest body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        UUID adminId = adminUserId(jwt);
        PaymentRailConfig cfg = configService.create(
                body.tenantId(), body.paymentMethod(), body.processorId(),
                body.priority(), body.configJson(), adminId, clientIp(httpRequest));
        return ResponseEntity.status(201)
                .body(PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId())));
    }

    @PutMapping("/rails/{id}/config")
    public PaymentRailConfigResponse updateConfig(
            @PathVariable UUID id,
            @RequestBody Map<String, Object> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        String  configJson = body.containsKey("configJson") ? (String) body.get("configJson") : null;
        Integer priority   = body.containsKey("priority")   ? ((Number) body.get("priority")).intValue() : null;
        PaymentRailConfig cfg = configService.updateConfig(id, configJson, priority, adminUserId(jwt), clientIp(httpRequest));
        return PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId()));
    }

    @PutMapping("/rails/{id}/processor")
    public PaymentRailConfigResponse updateProcessor(
            @PathVariable UUID id,
            @RequestBody Map<String, String> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        String processorId = body.get("processorId");
        if (processorId == null || processorId.isBlank()) {
            throw FixPayException.badRequest("processorId is required");
        }
        PaymentRailConfig cfg = configService.updateProcessorId(id, processorId, adminUserId(jwt), clientIp(httpRequest));
        return PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId()));
    }

    @PatchMapping("/rails/{id}/enabled")
    public PaymentRailConfigResponse toggleEnabled(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        PaymentRailConfig cfg = configService.toggleEnabled(id, enabled, adminUserId(jwt), clientIp(httpRequest));
        return PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId()));
    }

    @PatchMapping("/rails/{id}/maintenance")
    public PaymentRailConfigResponse setMaintenance(
            @PathVariable UUID id,
            @RequestBody Map<String, Boolean> body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        boolean maintenance = Boolean.TRUE.equals(body.get("maintenance"));
        PaymentRailConfig cfg = configService.setMaintenance(id, maintenance, adminUserId(jwt), clientIp(httpRequest));
        return PaymentRailConfigResponse.from(cfg, schemaFor(cfg.getProcessorId()));
    }

    @DeleteMapping("/rails/{id}")
    public ResponseEntity<Void> deleteRail(
            @PathVariable UUID id,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        configService.delete(id, adminUserId(jwt), clientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // ─── Discovery ────────────────────────────────────────────────────────────

    @GetMapping("/rails/processors")
    public List<String> listProcessors() {
        return registry.registeredProcessorIds();
    }

    @GetMapping("/rails/processors/{processorId}/schema")
    public ConfigSchema getProcessorSchema(@PathVariable String processorId) {
        ConfigSchema schema = schemaFor(processorId);
        if (schema == null) {
            throw FixPayException.notFound("Processor '" + processorId + "'");
        }
        return schema;
    }

    @GetMapping("/rails/health")
    public List<PaymentRailRegistry.ProcessorHealthStatus> getHealth() {
        return registry.getProcessorHealthSummaries();
    }

    // ─── Fee schedule ─────────────────────────────────────────────────────────

    @GetMapping("/rails/{id}/fees")
    public List<ProcessorFeeSchedule> listFees(@PathVariable UUID id) {
        return feeRepository.findByRailConfigIdOrderByEffectiveFromDesc(id);
    }

    @PostMapping("/rails/{id}/fees")
    public ResponseEntity<ProcessorFeeSchedule> addFee(
            @PathVariable UUID id,
            @RequestBody FeeScheduleRequest body,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        PaymentRailConfig cfg = configService.getById(id);
        ProcessorFeeSchedule fee = feeRepository.save(new ProcessorFeeSchedule(
                cfg,
                body.feeType(),
                body.fixedFeeKobo(),
                body.percentageFee(),
                body.capKobo(),
                body.minFeeKobo(),
                body.effectiveFrom() != null ? body.effectiveFrom() : LocalDate.now(),
                body.effectiveTo()
        ));
        auditService.record(adminUserId(jwt), RailAdminAuditService.ACTION_ADD_FEE_SCHEDULE,
                "ProcessorFeeSchedule", fee.getId(), null, fee, clientIp(httpRequest));
        return ResponseEntity.status(201).body(fee);
    }

    @DeleteMapping("/rails/{id}/fees/{feeId}")
    public ResponseEntity<Void> deleteFee(
            @PathVariable UUID id,
            @PathVariable UUID feeId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        ProcessorFeeSchedule fee = feeRepository.findById(feeId)
                .orElseThrow(() -> FixPayException.notFound("FeeSchedule"));
        feeRepository.delete(fee);
        auditService.record(adminUserId(jwt), RailAdminAuditService.ACTION_DELETE_FEE_SCHEDULE,
                "ProcessorFeeSchedule", feeId, null, null, clientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    // ─── Plugin management ────────────────────────────────────────────────────

    @GetMapping("/plugins")
    public List<Map<String, String>> listPlugins() {
        if (pluginManager == null) return List.of();
        return pluginManager.getLoadedPlugins().stream()
                .map(pw -> Map.of(
                        "pluginId", pw.getPluginId(),
                        "version",  pw.getDescriptor().getVersion(),
                        "state",    pw.getPluginState().toString()
                ))
                .collect(Collectors.toList());
    }

    @DeleteMapping("/plugins/{pluginId}")
    public ResponseEntity<Void> unloadPlugin(
            @PathVariable String pluginId,
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        if (pluginManager == null) {
            throw FixPayException.serviceUnavailable("Plugin management is not enabled");
        }
        boolean unloaded = pluginManager.unloadPlugin(pluginId);
        if (!unloaded) {
            throw FixPayException.notFound("Plugin '" + pluginId + "'");
        }
        registry.refreshPluginAdapters();
        auditService.record(adminUserId(jwt), RailAdminAuditService.ACTION_PLUGIN_UNLOAD,
                "Plugin", null, null, Map.of("pluginId", pluginId), clientIp(httpRequest));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/plugins/reload")
    public Map<String, Object> reloadPlugins(
            @AuthenticationPrincipal Jwt jwt,
            HttpServletRequest httpRequest) {
        if (pluginManager == null) {
            throw FixPayException.serviceUnavailable("Plugin management is not enabled");
        }
        pluginManager.loadPlugins();
        registry.refreshPluginAdapters();
        auditService.record(adminUserId(jwt), RailAdminAuditService.ACTION_PLUGIN_LOAD,
                "Plugin", null, null, null, clientIp(httpRequest));
        return Map.of("loaded", pluginManager.getLoadedPlugins().size());
    }

    // ─── Audit log ────────────────────────────────────────────────────────────

    @GetMapping("/rails/audit")
    public Page<PaymentRailAuditLog> getAuditLog(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.getAuditLog(PageRequest.of(page, size));
    }

    @GetMapping("/rails/{id}/audit")
    public Page<PaymentRailAuditLog> getEntityAuditLog(
            @PathVariable UUID id,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {
        return auditService.getAuditLogForEntity(id, PageRequest.of(page, size));
    }

    // ─── Settlement ───────────────────────────────────────────────────────────

    @GetMapping("/settlement/report")
    public SettlementReportService.SettlementReportResult getSettlementReport(
            @RequestParam UUID tenantId,
            @RequestParam String from,
            @RequestParam String to) {
        return settlementService.generateReport(tenantId, LocalDate.parse(from), LocalDate.parse(to));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private ConfigSchema schemaFor(String processorId) {
        return registry.resolveById(processorId)
                .map(PaymentRailAdapter::configSchema)
                .orElse(null);
    }

    private UUID adminUserId(Jwt jwt) {
        try { return UUID.fromString(jwt.getSubject()); } catch (Exception e) { return null; }
    }

    private String clientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
