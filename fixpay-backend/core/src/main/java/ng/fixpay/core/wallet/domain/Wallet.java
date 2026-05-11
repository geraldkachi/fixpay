package ng.fixpay.core.wallet.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallets")
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "ledger_balance", nullable = false, precision = 18, scale = 2)
    private BigDecimal ledgerBalance = BigDecimal.ZERO;

    @Column(nullable = false, length = 20)
    private String status = "active";

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Wallet() {}

    public Wallet(UUID userId, UUID tenantId) {
        this.userId   = userId;
        this.tenantId = tenantId;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ─── Getters ────────────────────────────────────────────────────────────

    public UUID getId()              { return id; }
    public UUID getUserId()          { return userId; }
    public UUID getTenantId()        { return tenantId; }
    public String getCurrency()      { return currency; }
    public BigDecimal getBalance()   { return balance; }
    public BigDecimal getLedgerBalance() { return ledgerBalance; }
    public String getStatus()        { return status; }
    public Instant getCreatedAt()    { return createdAt; }
    public Instant getUpdatedAt()    { return updatedAt; }

    public void setBalance(BigDecimal balance) {
        this.balance = balance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public void setStatus(String status) { this.status = status; }
}
