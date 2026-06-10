package ng.fixpay.core.ledger.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Immutable double-entry ledger row.
 *
 * <p>Every change to a wallet balance is recorded here as two rows:
 * <ol>
 *   <li>A DEBIT row on the user's wallet account (amount is positive, reduces balance).</li>
 *   <li>A CREDIT row on the counterpart account — revenue, provider, or reversal.</li>
 * </ol>
 *
 * <p>The table is append-only — rows are never updated or deleted.
 * Running balance is the sum of all CREDIT entries minus all DEBIT entries
 * for a given (walletId, currency) pair.
 *
 * <p>Columns:
 * <ul>
 *   <li>{@code entry_type}: DEBIT or CREDIT</li>
 *   <li>{@code amount}: always positive; direction is inferred from entry_type</li>
 *   <li>{@code running_balance}: balance <em>after</em> this row is applied</li>
 *   <li>{@code correlation_id}: links the two legs of a double-entry pair</li>
 *   <li>{@code reference}: payment reference or business reference</li>
 *   <li>{@code description}: human-readable reason</li>
 * </ul>
 */
@Entity
@Table(name = "ledger_entries",
        indexes = {
            @Index(name = "idx_ledger_wallet_created",   columnList = "wallet_id, created_at"),
            @Index(name = "idx_ledger_correlation",      columnList = "correlation_id"),
            @Index(name = "idx_ledger_reference",        columnList = "reference"),
        })
public class LedgerEntry {

    public enum EntryType { DEBIT, CREDIT }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "wallet_id", nullable = false)
    private UUID walletId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    /** Links the DEBIT and CREDIT legs of the same double-entry pair. */
    @Column(name = "correlation_id", nullable = false, length = 100)
    private String correlationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 10)
    private EntryType entryType;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    /** Balance of this wallet immediately after this entry is applied. */
    @Column(name = "running_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal runningBalance;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    /** Payment reference, mandate reference, or any business key. */
    @Column(nullable = false, length = 150)
    private String reference;

    @Column(nullable = false, length = 255)
    private String description;

    /** Append-only — never updated. */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected LedgerEntry() {}

    public LedgerEntry(
            UUID walletId,
            UUID userId,
            UUID tenantId,
            String correlationId,
            EntryType entryType,
            BigDecimal amount,
            BigDecimal runningBalance,
            String currency,
            String reference,
            String description
    ) {
        this.walletId       = walletId;
        this.userId         = userId;
        this.tenantId       = tenantId;
        this.correlationId  = correlationId;
        this.entryType      = entryType;
        this.amount         = amount;
        this.runningBalance = runningBalance;
        this.currency       = currency;
        this.reference      = reference;
        this.description    = description;
    }

    // ─── Getters (no setters — immutable) ────────────────────────────────────

    public UUID        getId()             { return id; }
    public UUID        getWalletId()       { return walletId; }
    public UUID        getUserId()         { return userId; }
    public UUID        getTenantId()       { return tenantId; }
    public String      getCorrelationId()  { return correlationId; }
    public EntryType   getEntryType()      { return entryType; }
    public BigDecimal  getAmount()         { return amount; }
    public BigDecimal  getRunningBalance() { return runningBalance; }
    public String      getCurrency()       { return currency; }
    public String      getReference()      { return reference; }
    public String      getDescription()    { return description; }
    public Instant     getCreatedAt()      { return createdAt; }
}
