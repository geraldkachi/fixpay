package ng.fixpay.core.payment.rail;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.payment.rail.domain.PaymentRailAuditLog;
import ng.fixpay.core.payment.rail.domain.PaymentRailAuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Service for writing and reading the payment rail admin audit log.
 *
 * <p>All write operations run in a separate transaction ({@code REQUIRES_NEW}) so
 * that an audit record is always committed even when the surrounding business
 * transaction rolls back.
 *
 * <h3>Predefined action constants</h3>
 * <p>Use the {@code ACTION_*} constants to keep action names consistent across callers.
 */
@Service
public class RailAdminAuditService {

    private static final Logger log = LoggerFactory.getLogger(RailAdminAuditService.class);

    // Standard action constants — extend as needed
    public static final String ACTION_CREATE_RAIL        = "CREATE_RAIL";
    public static final String ACTION_UPDATE_PROCESSOR   = "UPDATE_PROCESSOR";
    public static final String ACTION_UPDATE_CONFIG      = "UPDATE_CONFIG";
    public static final String ACTION_TOGGLE_ENABLED     = "TOGGLE_ENABLED";
    public static final String ACTION_SET_MAINTENANCE    = "SET_MAINTENANCE";
    public static final String ACTION_DELETE_RAIL        = "DELETE_RAIL";
    public static final String ACTION_ADD_FEE_SCHEDULE   = "ADD_FEE_SCHEDULE";
    public static final String ACTION_UPDATE_FEE_SCHEDULE = "UPDATE_FEE_SCHEDULE";
    public static final String ACTION_DELETE_FEE_SCHEDULE = "DELETE_FEE_SCHEDULE";
    public static final String ACTION_PLUGIN_LOAD        = "PLUGIN_LOAD";
    public static final String ACTION_PLUGIN_UNLOAD      = "PLUGIN_UNLOAD";
    public static final String ACTION_CB_FORCE_OPEN      = "CB_FORCE_OPEN";
    public static final String ACTION_CB_FORCE_CLOSE     = "CB_FORCE_CLOSE";

    private final PaymentRailAuditLogRepository repository;
    private final ObjectMapper objectMapper;

    public RailAdminAuditService(PaymentRailAuditLogRepository repository,
                                  ObjectMapper objectMapper) {
        this.repository   = repository;
        this.objectMapper = objectMapper;
    }

    // ─── Write ────────────────────────────────────────────────────────────────

    /**
     * Records an audit log entry.
     *
     * @param adminUserId    UUID of the admin (nullable for system actions)
     * @param action         one of the {@code ACTION_*} constants
     * @param entityType     entity class name (e.g. "PaymentRailConfig")
     * @param entityId       ID of the affected entity row
     * @param beforeSnapshot object state before change (serialised to JSON), or null
     * @param afterSnapshot  object state after change (serialised to JSON), or null
     * @param ipAddress      requester IP address, or null
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID adminUserId, String action, String entityType,
                       UUID entityId, Object beforeSnapshot, Object afterSnapshot,
                       String ipAddress) {
        try {
            String before = toJson(beforeSnapshot);
            String after  = toJson(afterSnapshot);
            PaymentRailAuditLog entry = new PaymentRailAuditLog(
                    adminUserId, action, entityType, entityId, before, after, ipAddress);
            repository.save(entry);
        } catch (Exception e) {
            // Audit failure must never break the primary operation.
            log.error("[RailAdminAuditService] Failed to persist audit log: action={} entity={}/{}",
                    action, entityType, entityId, e);
        }
    }

    /** Convenience overload without IP address or entity snapshots (e.g. for simple flags). */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(UUID adminUserId, String action, String entityType, UUID entityId) {
        record(adminUserId, action, entityType, entityId, null, null, null);
    }

    // ─── Read ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<PaymentRailAuditLog> getAuditLog(Pageable pageable) {
        return repository.findAllByOrderByCreatedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRailAuditLog> getAuditLogForEntity(UUID entityId, Pageable pageable) {
        return repository.findByEntityIdOrderByCreatedAtDesc(entityId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<PaymentRailAuditLog> getAuditLogForAdmin(UUID adminUserId, Pageable pageable) {
        return repository.findByAdminUserIdOrderByCreatedAtDesc(adminUserId, pageable);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private String toJson(Object obj) {
        if (obj == null) return null;
        if (obj instanceof String s) return s;
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            log.warn("[RailAdminAuditService] Failed to serialise snapshot: {}", e.getMessage());
            return obj.toString();
        }
    }
}
