package ng.fixpay.core.mandate.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "nibss_mandates")
public class NibssMandate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "mandate_reference", nullable = false, unique = true, length = 120)
    private String mandateReference;

    @Column(name = "bank_code", nullable = false, length = 10)
    private String bankCode;

    @Column(name = "account_number", nullable = false, length = 20)
    private String accountNumber;

    @Column(name = "max_amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal maxAmount;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Column(name = "provider_reference", length = 120)
    private String providerReference;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected NibssMandate() {}

    public NibssMandate(
            UUID userId,
            UUID tenantId,
            String mandateReference,
            String bankCode,
            String accountNumber,
            BigDecimal maxAmount,
            LocalDate startDate,
            LocalDate endDate
    ) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.mandateReference = mandateReference;
        this.bankCode = bankCode;
        this.accountNumber = accountNumber;
        this.maxAmount = maxAmount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = "pending";
        this.providerMessage = "Mandate created and awaiting provider activation";
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getMandateReference() { return mandateReference; }
    public String getBankCode() { return bankCode; }
    public String getAccountNumber() { return accountNumber; }
    public BigDecimal getMaxAmount() { return maxAmount; }
    public String getStatus() { return status; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public String getProviderMessage() { return providerMessage; }
    public String getProviderReference() { return providerReference; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void updateStatus(String status, String providerMessage, String providerReference) {
        this.status = status;
        this.providerMessage = providerMessage;
        this.providerReference = providerReference;
    }
}
