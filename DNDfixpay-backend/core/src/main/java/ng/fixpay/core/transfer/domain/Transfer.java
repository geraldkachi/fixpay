package ng.fixpay.core.transfer.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "transfers",
        indexes = {
            @Index(name = "idx_transfers_user_id",    columnList = "user_id"),
            @Index(name = "idx_transfers_reference",  columnList = "reference"),
            @Index(name = "idx_transfers_created_at", columnList = "created_at"),
        })
public class Transfer {

    public enum TransferType   { BANK, WALLET }
    public enum TransferStatus { initiated, processing, completed, failed, reversed }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 80)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(name = "transfer_type", nullable = false, length = 10)
    private TransferType transferType;

    // ── Bank transfer fields ──────────────────────────────────────────────────

    @Column(name = "recipient_account_number", length = 20)
    private String recipientAccountNumber;

    @Column(name = "recipient_bank_code", length = 10)
    private String recipientBankCode;

    @Column(name = "recipient_bank_name", length = 100)
    private String recipientBankName;

    @Column(name = "recipient_account_name", length = 150)
    private String recipientAccountName;

    // ── Wallet (P2P) transfer fields ──────────────────────────────────────────

    @Column(name = "recipient_user_id")
    private UUID recipientUserId;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    // ── Common ────────────────────────────────────────────────────────────────

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal fee;

    @Column(length = 255)
    private String narration;

    @Column(nullable = false, length = 30)
    private String status = TransferStatus.initiated.name();

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "provider_response", columnDefinition = "TEXT")
    private String providerResponse;

    @Column(name = "failure_reason", length = 255)
    private String failureReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Transfer() {}

    public Transfer(UUID userId, UUID tenantId, String reference, TransferType transferType,
                    BigDecimal amount, BigDecimal fee, String narration) {
        this.userId       = userId;
        this.tenantId     = tenantId;
        this.reference    = reference;
        this.transferType = transferType;
        this.amount       = amount;
        this.fee          = fee;
        this.narration    = narration;
    }

    // ── Mutators ──────────────────────────────────────────────────────────────

    public Transfer withBankRecipient(String accountNumber, String bankCode, String bankName, String accountName) {
        this.recipientAccountNumber = accountNumber;
        this.recipientBankCode      = bankCode;
        this.recipientBankName      = bankName;
        this.recipientAccountName   = accountName;
        return this;
    }

    public Transfer withWalletRecipient(UUID recipientUserId, String recipientPhone) {
        this.recipientUserId = recipientUserId;
        this.recipientPhone  = recipientPhone;
        return this;
    }

    public void markProcessing(String providerReference) {
        this.status            = TransferStatus.processing.name();
        this.providerReference = providerReference;
        this.updatedAt         = Instant.now();
    }

    public void markCompleted(String providerResponse) {
        this.status           = TransferStatus.completed.name();
        this.providerResponse = providerResponse;
        this.updatedAt        = Instant.now();
    }

    public void markFailed(String reason) {
        this.status        = TransferStatus.failed.name();
        this.failureReason = reason;
        this.updatedAt     = Instant.now();
    }

    public void markReversed() {
        this.status    = TransferStatus.reversed.name();
        this.updatedAt = Instant.now();
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public UUID           getId()                     { return id; }
    public UUID           getUserId()                 { return userId; }
    public UUID           getTenantId()               { return tenantId; }
    public String         getReference()              { return reference; }
    public TransferType   getTransferType()           { return transferType; }
    public String         getRecipientAccountNumber() { return recipientAccountNumber; }
    public String         getRecipientBankCode()      { return recipientBankCode; }
    public String         getRecipientBankName()      { return recipientBankName; }
    public String         getRecipientAccountName()   { return recipientAccountName; }
    public UUID           getRecipientUserId()        { return recipientUserId; }
    public String         getRecipientPhone()         { return recipientPhone; }
    public BigDecimal     getAmount()                 { return amount; }
    public BigDecimal     getFee()                    { return fee; }
    public String         getNarration()              { return narration; }
    public String         getStatus()                 { return status; }
    public String         getProviderReference()      { return providerReference; }
    public String         getProviderResponse()       { return providerResponse; }
    public String         getFailureReason()          { return failureReason; }
    public Instant        getCreatedAt()              { return createdAt; }
    public Instant        getUpdatedAt()              { return updatedAt; }
}
