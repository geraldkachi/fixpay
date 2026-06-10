package ng.fixpay.core.kyc.domain;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "kyc_verifications")
public class KycVerification {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "cac_registration_number", nullable = false)
    private String cacRegistrationNumber;

    @Column(name = "directors_json", nullable = false, columnDefinition = "TEXT")
    private String directorsJson;

    @Column(name = "verification_status", nullable = false, length = 20)
    private String verificationStatus;

    @Column(name = "provider_reference", length = 100)
    private String providerReference;

    @Column(name = "report_url", length = 500)
    private String reportUrl;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected KycVerification() {}

    public KycVerification(UUID userId, UUID tenantId, String companyName, String cacRegistrationNumber, String directorsJson) {
        this.userId = userId;
        this.tenantId = tenantId;
        this.companyName = companyName;
        this.cacRegistrationNumber = cacRegistrationNumber;
        this.directorsJson = directorsJson;
        this.verificationStatus = "pending";
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public UUID getTenantId() { return tenantId; }
    public String getCompanyName() { return companyName; }
    public String getCacRegistrationNumber() { return cacRegistrationNumber; }
    public String getDirectorsJson() { return directorsJson; }
    public String getVerificationStatus() { return verificationStatus; }
    public String getProviderReference() { return providerReference; }
    public String getReportUrl() { return reportUrl; }
    public Instant getVerifiedAt() { return verifiedAt; }
    public Instant getCreatedAt() { return createdAt; }

    public void markVerified(String providerReference, String reportUrl, Instant verifiedAt) {
        this.verificationStatus = "verified";
        this.providerReference = providerReference;
        this.reportUrl = reportUrl;
        this.verifiedAt = verifiedAt;
    }

    public void markFailed(String providerReference, String reportUrl, Instant verifiedAt) {
        this.verificationStatus = "failed";
        this.providerReference = providerReference;
        this.reportUrl = reportUrl;
        this.verifiedAt = verifiedAt;
    }
}
