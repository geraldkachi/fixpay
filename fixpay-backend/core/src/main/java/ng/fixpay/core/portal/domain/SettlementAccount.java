package ng.fixpay.core.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "settlement_accounts")
public class SettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "bank_name", nullable = false, length = 120)
    private String bankName;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "account_name", nullable = false, length = 255)
    private String accountName;

    @Column(nullable = false, length = 3)
    private String currency = "NGN";

    @Column(nullable = false)
    private boolean verified = false;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected SettlementAccount() {}

    public SettlementAccount(UUID tenantId, String bankCode, String bankName,
                             String accountNumber, String accountName, String currency) {
        this.tenantId      = tenantId;
        this.bankCode      = bankCode;
        this.bankName      = bankName;
        this.accountNumber = accountNumber;
        this.accountName   = accountName;
        this.currency      = currency;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID    getId()            { return id; }
    public UUID    getTenantId()      { return tenantId; }
    public String  getBankCode()      { return bankCode; }
    public String  getBankName()      { return bankName; }
    public String  getAccountNumber() { return accountNumber; }
    public String  getAccountName()   { return accountName; }
    public String  getCurrency()      { return currency; }
    public boolean isVerified()       { return verified; }
    public Instant getVerifiedAt()    { return verifiedAt; }
    public Instant getCreatedAt()     { return createdAt; }
    public Instant getUpdatedAt()     { return updatedAt; }

    public void setBankCode(String bankCode)           { this.bankCode = bankCode; }
    public void setBankName(String bankName)           { this.bankName = bankName; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public void setAccountName(String accountName)     { this.accountName = accountName; }
    public void markVerified()                         { this.verified = true; this.verifiedAt = Instant.now(); }
}

