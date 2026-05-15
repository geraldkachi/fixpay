package ng.fixpay.core.payment.rail.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Fee schedule for a payment rail processor configuration.
 *
 * <p>Stores the platform's cost for using a specific processor on a given payment method.
 * Multiple rows per {@link PaymentRailConfig} are supported with effective date ranges,
 * allowing scheduled fee changes without code deployments.
 *
 * <h3>Fee calculation</h3>
 * <pre>
 *   fee_kobo = max(min_fee_kobo,
 *                  min(cap_kobo,
 *                      fixed_fee_kobo + amount_kobo × percentage_fee))
 * </pre>
 *
 * <p>The computed fee is stored in {@code vtpass_payments.processor_fee_kobo} and
 * used by the billing/settlement module to calculate real-time platform revenue.
 */
@Entity
@Table(name = "processor_fee_schedule",
       indexes = @Index(name = "idx_fee_schedule_config_date",
                        columnList = "payment_rail_config_id,effective_from,effective_to"))
public class ProcessorFeeSchedule {

    /** Fee types supported by the calculator. */
    public enum FeeType { FIXED, PERCENTAGE, TIERED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "payment_rail_config_id", nullable = false)
    private PaymentRailConfig railConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 20)
    private FeeType feeType = FeeType.TIERED;

    /** Fixed fee component in kobo (e.g. 5000 = ₦50). */
    @Column(name = "fixed_fee_kobo", nullable = false)
    private long fixedFeeKobo = 0L;

    /**
     * Percentage fee as a decimal (e.g. 0.015000 = 1.5%).
     * Stored with 6 decimal places for precision.
     */
    @Column(name = "percentage_fee", nullable = false, precision = 8, scale = 6)
    private BigDecimal percentageFee = BigDecimal.ZERO;

    /** Maximum fee cap in kobo. {@code null} means no ceiling. */
    @Column(name = "cap_kobo")
    private Long capKobo;

    /** Minimum fee floor in kobo. */
    @Column(name = "min_fee_kobo", nullable = false)
    private long minFeeKobo = 0L;

    /** Inclusive start date for this fee schedule row. */
    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom = LocalDate.now();

    /** Inclusive end date. {@code null} means indefinitely active. */
    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected ProcessorFeeSchedule() {}

    public ProcessorFeeSchedule(PaymentRailConfig railConfig, FeeType feeType,
                                  long fixedFeeKobo, BigDecimal percentageFee,
                                  Long capKobo, long minFeeKobo,
                                  LocalDate effectiveFrom, LocalDate effectiveTo) {
        this.railConfig    = railConfig;
        this.feeType       = feeType;
        this.fixedFeeKobo  = fixedFeeKobo;
        this.percentageFee = percentageFee != null ? percentageFee : BigDecimal.ZERO;
        this.capKobo       = capKobo;
        this.minFeeKobo    = minFeeKobo;
        this.effectiveFrom = effectiveFrom != null ? effectiveFrom : LocalDate.now();
        this.effectiveTo   = effectiveTo;
    }

    // ─── Accessors ────────────────────────────────────────────────────────────

    public UUID getId()                  { return id; }
    public PaymentRailConfig getRailConfig() { return railConfig; }
    public FeeType getFeeType()          { return feeType; }
    public long getFixedFeeKobo()        { return fixedFeeKobo; }
    public BigDecimal getPercentageFee() { return percentageFee; }
    public Long getCapKobo()             { return capKobo; }
    public long getMinFeeKobo()          { return minFeeKobo; }
    public LocalDate getEffectiveFrom()  { return effectiveFrom; }
    public LocalDate getEffectiveTo()    { return effectiveTo; }
    public Instant getCreatedAt()        { return createdAt; }
    public Instant getUpdatedAt()        { return updatedAt; }

    // ─── Mutators ─────────────────────────────────────────────────────────────

    public void setFeeType(FeeType feeType)              { this.feeType = feeType; }
    public void setFixedFeeKobo(long fixedFeeKobo)       { this.fixedFeeKobo = fixedFeeKobo; }
    public void setPercentageFee(BigDecimal pct)         { this.percentageFee = pct; }
    public void setCapKobo(Long capKobo)                 { this.capKobo = capKobo; }
    public void setMinFeeKobo(long minFeeKobo)           { this.minFeeKobo = minFeeKobo; }
    public void setEffectiveFrom(LocalDate effectiveFrom){ this.effectiveFrom = effectiveFrom; }
    public void setEffectiveTo(LocalDate effectiveTo)    { this.effectiveTo = effectiveTo; }
    public void touch()                                  { this.updatedAt = Instant.now(); }
}
