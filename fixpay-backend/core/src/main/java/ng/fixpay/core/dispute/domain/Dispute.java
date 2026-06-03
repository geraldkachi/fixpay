package ng.fixpay.core.dispute.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "disputes",
        indexes = {
            @Index(name = "idx_disputes_user_id",    columnList = "user_id"),
            @Index(name = "idx_disputes_created_at", columnList = "created_at"),
        })
public class Dispute {

    public enum Category { WRONG_AMOUNT, NOT_RECEIVED, DOUBLE_CHARGE, UNAUTHORIZED, OTHER }
    public enum Status   { open, in_review, resolved, closed }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "transaction_reference", length = 80)
    private String transactionReference;

    // Snapshot of the disputed transaction (immutable at dispute time)
    @Column(name = "transaction_description", length = 255)
    private String transactionDescription;

    @Column(name = "transaction_amount", precision = 18, scale = 2)
    private BigDecimal transactionAmount;

    @Column(name = "transaction_date")
    private Instant transactionDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Category category;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.open;

    @Column(columnDefinition = "TEXT")
    private String resolution;

    /** CBN 5-day resolution SLA from raise date. */
    @Column(name = "sla_deadline", nullable = false)
    private Instant slaDeadline;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Dispute() {}

    public Dispute(UUID userId, UUID tenantId, String transactionReference,
                   String transactionDescription, BigDecimal transactionAmount,
                   Instant transactionDate, Category category, String description) {
        this.userId                  = userId;
        this.tenantId                = tenantId;
        this.transactionReference    = transactionReference;
        this.transactionDescription  = transactionDescription;
        this.transactionAmount       = transactionAmount;
        this.transactionDate         = transactionDate;
        this.category                = category;
        this.description             = description;
        // CBN SLA: 5 calendar days
        this.slaDeadline             = Instant.now().plusSeconds(5L * 24 * 60 * 60);
    }

    public void resolve(String resolution) {
        this.resolution = resolution;
        this.status     = Status.resolved;
        this.updatedAt  = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID        getId()                    { return id; }
    public UUID        getUserId()                { return userId; }
    public UUID        getTenantId()              { return tenantId; }
    public String      getTransactionReference()  { return transactionReference; }
    public String      getTransactionDescription() { return transactionDescription; }
    public BigDecimal  getTransactionAmount()     { return transactionAmount; }
    public Instant     getTransactionDate()       { return transactionDate; }
    public Category    getCategory()              { return category; }
    public String      getDescription()           { return description; }
    public Status      getStatus()                { return status; }
    public String      getResolution()            { return resolution; }
    public Instant     getSlaDeadline()           { return slaDeadline; }
    public Instant     getCreatedAt()             { return createdAt; }
    public Instant     getUpdatedAt()             { return updatedAt; }
}
