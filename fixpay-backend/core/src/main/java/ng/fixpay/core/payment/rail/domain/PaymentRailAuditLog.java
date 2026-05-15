package ng.fixpay.core.payment.rail.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Immutable audit record for every admin action on payment rail configuration.
 *
 * <p>Created by {@link ng.fixpay.core.payment.rail.RailAdminAuditService} on every
 * create / update / delete / toggle / maintenance operation. Used for compliance,
 * dispute investigation, and change-history display in the admin wizard.
 */
@Entity
@Table(name = "payment_rail_audit_log",
       indexes = {
           @Index(name = "idx_audit_log_entity", columnList = "entity_id"),
           @Index(name = "idx_audit_log_admin",  columnList = "admin_user_id"),
           @Index(name = "idx_audit_log_recent", columnList = "created_at")
       })
public class PaymentRailAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /** ID of the admin user who performed the action. {@code null} for system-initiated changes. */
    @Column(name = "admin_user_id")
    private UUID adminUserId;

    /** Action performed, e.g. {@code CREATE_RAIL}, {@code UPDATE_CONFIG}, {@code TOGGLE_ENABLED}. */
    @Column(name = "action", nullable = false, length = 60)
    private String action;

    /** Entity class name, e.g. {@code PaymentRailConfig}. */
    @Column(name = "entity_type", nullable = false, length = 60)
    private String entityType = "PaymentRailConfig";

    /** ID of the affected entity row. */
    @Column(name = "entity_id")
    private UUID entityId;

    /** JSON snapshot of the entity state before the change. */
    @Column(name = "before_state_json", columnDefinition = "TEXT")
    private String beforeStateJson;

    /** JSON snapshot of the entity state after the change. */
    @Column(name = "after_state_json", columnDefinition = "TEXT")
    private String afterStateJson;

    /** IPv4 or IPv6 address of the admin making the request. */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected PaymentRailAuditLog() {}

    public PaymentRailAuditLog(UUID adminUserId, String action, String entityType,
                                UUID entityId, String beforeStateJson,
                                String afterStateJson, String ipAddress) {
        this.adminUserId    = adminUserId;
        this.action         = action;
        this.entityType     = entityType != null ? entityType : "PaymentRailConfig";
        this.entityId       = entityId;
        this.beforeStateJson = beforeStateJson;
        this.afterStateJson  = afterStateJson;
        this.ipAddress      = ipAddress;
        this.createdAt      = Instant.now();
    }

    public UUID getId()              { return id; }
    public UUID getAdminUserId()     { return adminUserId; }
    public String getAction()        { return action; }
    public String getEntityType()    { return entityType; }
    public UUID getEntityId()        { return entityId; }
    public String getBeforeStateJson() { return beforeStateJson; }
    public String getAfterStateJson()  { return afterStateJson; }
    public String getIpAddress()     { return ipAddress; }
    public Instant getCreatedAt()    { return createdAt; }
}
