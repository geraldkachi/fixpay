package ng.fixpay.core.payment.rail.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin-managed configuration that maps a payment method to a specific processor
 * for a given tenant.
 *
 * <p>The {@link ng.fixpay.core.payment.rail.PaymentRailRegistry} resolves the active
 * {@link ng.fixpay.shared.payment.PaymentRailAdapter} implementation by looking up
 * {@code (tenantId, paymentMethod, enabled=true)} ordered by {@code priority} (lower
 * value = higher priority). The first matching row is used; subsequent rows act as
 * fallbacks.
 *
 * <p>A {@code tenantId} of {@code null} denotes a <em>global default</em> config that
 * applies to all tenants that have no tenant-specific override for that method.
 */
@Entity
@Table(
    name = "payment_rail_config",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_rail_config_tenant_method_processor",
        columnNames = {"tenant_id", "payment_method", "processor_id"}
    )
)
public class PaymentRailConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Tenant this config applies to. {@code null} = global default (applies to all
     * tenants that have no tenant-specific override for the same payment method).
     */
    @Column(name = "tenant_id")
    private UUID tenantId;

    /** The payment method (funding channel) this row configures. */
    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    private VtpassPaymentMethod paymentMethod;

    /**
     * Matches {@link ng.fixpay.shared.payment.PaymentRailAdapter#processorId()}.
     * Examples: {@code internal-wallet}, {@code paystack-card}, {@code monnify-transfer}.
     */
    @Column(name = "processor_id", nullable = false, length = 60)
    private String processorId;

    /**
     * Lower value = tried first. When the primary processor (priority 1) is down, the
     * registry falls back to the next enabled row for the same method.
     */
    @Column(name = "priority", nullable = false)
    private int priority = 1;

    /** When false, this processor will never be selected even if it is the only option. */
    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    /**
     * When true, this processor is temporarily out of rotation (e.g. for maintenance or
     * manual circuit break). Takes effect immediately — the registry skips maintenance
     * configs regardless of circuit breaker state or priority.
     */
    @Column(name = "maintenance", nullable = false)
    private boolean maintenance = false;

    /**
     * Processor-specific key/value pairs stored as JSON (e.g. API keys, base URLs,
     * sandbox flags). Values should be encrypted at rest using application-level
     * encryption before storing sensitive credentials.
     *
     * <p>The registry decodes this blob and passes it as
     * {@link ng.fixpay.shared.payment.PaymentRailRequest#processorConfig()} so each
     * adapter receives only its own configuration.
     */
    @Column(name = "config_json", columnDefinition = "text")
    private String configJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    // ─── Constructors ─────────────────────────────────────────────────────

    protected PaymentRailConfig() {}

    public PaymentRailConfig(UUID tenantId, VtpassPaymentMethod paymentMethod,
                              String processorId, int priority, boolean enabled, String configJson) {
        this.tenantId = tenantId;
        this.paymentMethod = paymentMethod;
        this.processorId = processorId;
        this.priority = priority;
        this.enabled = enabled;
        this.configJson = configJson;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // ─── Accessors ────────────────────────────────────────────────────────

    public UUID getId() { return id; }
    public UUID getTenantId() { return tenantId; }
    public VtpassPaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getProcessorId() { return processorId; }
    public int getPriority() { return priority; }
    public boolean isEnabled() { return enabled; }
    public boolean isMaintenance() { return maintenance; }
    public String getConfigJson() { return configJson; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    // ─── Mutators (for admin API) ─────────────────────────────────────────

    public void setProcessorId(String processorId) { this.processorId = processorId; }
    public void setPriority(int priority) { this.priority = priority; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setMaintenance(boolean maintenance) { this.maintenance = maintenance; }
    public void setConfigJson(String configJson) { this.configJson = configJson; }
    public void touch() { this.updatedAt = Instant.now(); }
}
