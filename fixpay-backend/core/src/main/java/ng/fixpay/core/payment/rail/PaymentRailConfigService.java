package ng.fixpay.core.payment.rail;

import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfig;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfigRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Application service for admin management of {@link PaymentRailConfig} records.
 *
 * <p>Exposes CRUD operations consumed by {@link PaymentRailAdminController}. All
 * mutations are performed within a transaction. Audit records are written by
 * {@link RailAdminAuditService} inside each mutating method — in a REQUIRES_NEW
 * transaction so audit commits even on rollback.
 */
@Service
public class PaymentRailConfigService {

    private final PaymentRailConfigRepository repository;
    private final PaymentRailRegistry         registry;
    private final RailAdminAuditService       auditService;

    public PaymentRailConfigService(PaymentRailConfigRepository repository,
                                     PaymentRailRegistry registry,
                                     RailAdminAuditService auditService) {
        this.repository   = repository;
        this.registry     = registry;
        this.auditService = auditService;
    }

    // ─── Queries ──────────────────────────────────────────────────────────────

    public List<PaymentRailConfig> listForTenant(UUID tenantId) {
        List<PaymentRailConfig> rows = new ArrayList<>(
                repository.findByTenantIdOrderByPaymentMethodAscPriorityAsc(tenantId));
        // Append global defaults so the admin can see them even if they have no tenant rows
        rows.addAll(repository.findByTenantIdIsNullOrderByPaymentMethodAscPriorityAsc());
        return rows;
    }

    public PaymentRailConfig getById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> FixPayException.notFound("PaymentRailConfig"));
    }

    /** Returns all processor IDs currently registered in the application context. */
    public List<String> availableProcessors() {
        return registry.registeredProcessorIds();
    }

    // ─── Mutations ────────────────────────────────────────────────────────────

    @Transactional
    public PaymentRailConfig create(UUID tenantId, VtpassPaymentMethod paymentMethod,
                                     String processorId, int priority, String configJson,
                                     UUID adminUserId, String ipAddress) {
        validateProcessorId(processorId);
        PaymentRailConfig cfg = repository.save(
                new PaymentRailConfig(tenantId, paymentMethod, processorId, priority, true, configJson));
        auditService.record(adminUserId, RailAdminAuditService.ACTION_CREATE_RAIL,
                "PaymentRailConfig", cfg.getId(), null, cfg, ipAddress);
        return cfg;
    }

    @Transactional
    public PaymentRailConfig updateProcessorId(UUID id, String processorId,
                                                UUID adminUserId, String ipAddress) {
        validateProcessorId(processorId);
        PaymentRailConfig cfg = getById(id);
        java.util.Map<String,Object> before = snapshot(cfg);
        cfg.setProcessorId(processorId);
        cfg.touch();
        PaymentRailConfig saved = repository.save(cfg);
        auditService.record(adminUserId, RailAdminAuditService.ACTION_UPDATE_PROCESSOR,
                "PaymentRailConfig", id, before, saved, ipAddress);
        return saved;
    }

    @Transactional
    public PaymentRailConfig updateConfig(UUID id, String configJson, Integer priority,
                                           UUID adminUserId, String ipAddress) {
        PaymentRailConfig cfg = getById(id);
        java.util.Map<String,Object> before = snapshot(cfg);
        if (configJson != null) cfg.setConfigJson(configJson);
        if (priority  != null) cfg.setPriority(priority);
        cfg.touch();
        PaymentRailConfig saved = repository.save(cfg);
        auditService.record(adminUserId, RailAdminAuditService.ACTION_UPDATE_CONFIG,
                "PaymentRailConfig", id, before, saved, ipAddress);
        return saved;
    }

    @Transactional
    public PaymentRailConfig toggleEnabled(UUID id, boolean enabled,
                                            UUID adminUserId, String ipAddress) {
        PaymentRailConfig cfg = getById(id);
        cfg.setEnabled(enabled);
        cfg.touch();
        PaymentRailConfig saved = repository.save(cfg);
        auditService.record(adminUserId, RailAdminAuditService.ACTION_TOGGLE_ENABLED,
                "PaymentRailConfig", id, null,
                java.util.Map.of("enabled", enabled), ipAddress);
        return saved;
    }

    @Transactional
    public PaymentRailConfig setMaintenance(UUID id, boolean maintenance,
                                             UUID adminUserId, String ipAddress) {
        PaymentRailConfig cfg = getById(id);
        cfg.setMaintenance(maintenance);
        cfg.touch();
        PaymentRailConfig saved = repository.save(cfg);
        auditService.record(adminUserId, RailAdminAuditService.ACTION_SET_MAINTENANCE,
                "PaymentRailConfig", id, null,
                java.util.Map.of("maintenance", maintenance), ipAddress);
        return saved;
    }

    @Transactional
    public void delete(UUID id, UUID adminUserId, String ipAddress) {
        PaymentRailConfig cfg = getById(id);
        Object snapshot = snapshot(cfg);
        repository.delete(cfg);
        auditService.record(adminUserId, RailAdminAuditService.ACTION_DELETE_RAIL,
                "PaymentRailConfig", id, snapshot, null, ipAddress);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private void validateProcessorId(String processorId) {
        List<String> known = registry.registeredProcessorIds();
        if (!known.contains(processorId)) {
            throw FixPayException.badRequest(
                    "Unknown processorId '" + processorId + "'. Available: " + known);
        }
    }

    /** Creates a detached summary map for audit snapshots (avoids lazy-loading issues). */
    private java.util.Map<String, Object> snapshot(PaymentRailConfig cfg) {
        return java.util.Map.of(
                "id",          cfg.getId().toString(),
                "tenantId",    cfg.getTenantId() != null ? cfg.getTenantId().toString() : "",
                "processorId", cfg.getProcessorId(),
                "priority",    cfg.getPriority(),
                "enabled",     cfg.isEnabled(),
                "maintenance", cfg.isMaintenance()
        );
    }
}
