package ng.fixpay.core.payment.domain;

import jakarta.persistence.*;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "vtpass_payments")
public class VtpassPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "payment_reference", nullable = false, unique = true, length = 100)
    private String paymentReference;

    @Column(name = "service_id", nullable = false, length = 100)
    private String serviceId;

    @Column(name = "biller_customer_ref", nullable = false, length = 100)
    private String billerCustomerRef;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 20)
    private VtpassPaymentMethod paymentMethod;

    @Column(name = "payment_status", nullable = false, length = 30)
    private String paymentStatus;

    @Column(name = "provider_status", nullable = false, length = 30)
    private String providerStatus;

    @Column(name = "provider_code", length = 10)
    private String providerCode;

    @Column(name = "provider_message", length = 255)
    private String providerMessage;

    @Column(name = "external_reference", length = 150)
    private String externalReference;

    @Column(name = "mandate_reference", length = 120)
    private String mandateReference;

    @Column(name = "authorization_payload", columnDefinition = "TEXT")
    private String authorizationPayload;

    @Column(name = "init_idempotency_key", length = 120, unique = true)
    private String initIdempotencyKey;

    @Column(name = "last_execute_idempotency_key", length = 120)
    private String lastExecuteIdempotencyKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected VtpassPayment() {}

    public VtpassPayment(
            UUID userId,
            UUID tenantId,
            String paymentReference,
            String serviceId,
            String billerCustomerRef,
            BigDecimal amount,
            VtpassPaymentMethod paymentMethod,
                String mandateReference,
                String initIdempotencyKey
    ) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.paymentReference = paymentReference;
        this.serviceId = serviceId;
        this.billerCustomerRef = billerCustomerRef;
        this.amount = amount;
        this.paymentMethod = paymentMethod;
        this.mandateReference = mandateReference;
        this.initIdempotencyKey = initIdempotencyKey;
        this.paymentStatus = "initiated";
        this.providerStatus = "pending";
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getPaymentReference() { return paymentReference; }
    public String getServiceId() { return serviceId; }
    public String getBillerCustomerRef() { return billerCustomerRef; }
    public BigDecimal getAmount() { return amount; }
    public VtpassPaymentMethod getPaymentMethod() { return paymentMethod; }
    public String getPaymentStatus() { return paymentStatus; }
    public String getProviderStatus() { return providerStatus; }
    public String getProviderCode() { return providerCode; }
    public String getProviderMessage() { return providerMessage; }
    public String getExternalReference() { return externalReference; }
    public String getMandateReference() { return mandateReference; }
    public String getAuthorizationPayload() { return authorizationPayload; }
    public String getInitIdempotencyKey() { return initIdempotencyKey; }
    public String getLastExecuteIdempotencyKey() { return lastExecuteIdempotencyKey; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setLastExecuteIdempotencyKey(String lastExecuteIdempotencyKey) {
        this.lastExecuteIdempotencyKey = lastExecuteIdempotencyKey;
    }

    public void markAuthorized(String providerStatus, String externalReference, String authorizationPayload) {
        this.paymentStatus = "authorized";
        this.providerStatus = providerStatus;
        this.providerCode = null;
        this.providerMessage = null;
        this.externalReference = externalReference;
        this.authorizationPayload = authorizationPayload;
    }

    public void markPendingAuthorization(String providerStatus, String externalReference, String authorizationPayload) {
        this.paymentStatus = "pending_authorization";
        this.providerStatus = providerStatus;
        this.providerCode = null;
        this.providerMessage = null;
        this.externalReference = externalReference;
        this.authorizationPayload = authorizationPayload;
    }

    public void markProcessing(String providerStatus, String providerCode, String providerMessage, String externalReference) {
        this.paymentStatus = "processing";
        this.providerStatus = providerStatus;
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
        this.externalReference = externalReference;
    }

    public void markCompleted(String providerStatus, String providerCode, String providerMessage, String externalReference) {
        this.paymentStatus = "completed";
        this.providerStatus = providerStatus;
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
        this.externalReference = externalReference;
    }

    public void markFailed(String providerStatus, String providerCode, String providerMessage, String externalReference) {
        this.paymentStatus = "failed";
        this.providerStatus = providerStatus;
        this.providerCode = providerCode;
        this.providerMessage = providerMessage;
        this.externalReference = externalReference;
    }
}
