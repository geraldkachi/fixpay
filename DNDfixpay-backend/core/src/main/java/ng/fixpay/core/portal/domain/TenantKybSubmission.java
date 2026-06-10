package ng.fixpay.core.portal.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Entity
@Table(name = "tenant_kyb_submissions")
public class TenantKybSubmission {

    public enum Status { DRAFT, SUBMITTED, APPROVED, REJECTED }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, unique = true)
    private UUID tenantId;

    @Column(name = "cac_number", length = 30)
    private String cacNumber;

    @Column(name = "business_type", length = 50)
    private String businessType;

    @Column(name = "registered_address")
    private String registeredAddress;

    @Column(length = 60)
    private String state;

    @Column(length = 60)
    private String country = "Nigeria";

    @Convert(converter = JsonConverters.DirectorsConverter.class)
    @Column(columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private List<Map<String, String>> directors;

    @Convert(converter = JsonConverters.StringMapConverter.class)
    @Column(name = "document_urls", columnDefinition = "jsonb")
    @ColumnTransformer(write = "?::jsonb")
    private Map<String, String> documentUrls;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(name = "review_notes", columnDefinition = "TEXT")
    private String reviewNotes;

    @Column(name = "submitted_at")
    private Instant submittedAt;

    @Column(name = "reviewed_by")
    private UUID reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected TenantKybSubmission() {}

    public TenantKybSubmission(UUID tenantId) {
        this.tenantId = tenantId;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID   getId()                { return id; }
    public UUID   getTenantId()          { return tenantId; }
    public String getCacNumber()         { return cacNumber; }
    public String getBusinessType()      { return businessType; }
    public String getRegisteredAddress() { return registeredAddress; }
    public String getState()             { return state; }
    public String getCountry()           { return country; }
    public List<Map<String, String>> getDirectors()    { return directors; }
    public Map<String, String>       getDocumentUrls() { return documentUrls; }
    public Status  getStatus()          { return status; }
    public String  getReviewNotes()     { return reviewNotes; }
    public Instant getSubmittedAt()     { return submittedAt; }
    public UUID    getReviewedBy()      { return reviewedBy; }
    public Instant getReviewedAt()      { return reviewedAt; }
    public Instant getCreatedAt()       { return createdAt; }
    public Instant getUpdatedAt()       { return updatedAt; }

    public void setCacNumber(String cacNumber)                   { this.cacNumber = cacNumber; }
    public void setBusinessType(String businessType)             { this.businessType = businessType; }
    public void setRegisteredAddress(String registeredAddress)   { this.registeredAddress = registeredAddress; }
    public void setState(String state)                           { this.state = state; }
    public void setDirectors(List<Map<String, String>> directors){ this.directors = directors; }
    public void setDocumentUrls(Map<String, String> documentUrls){ this.documentUrls = documentUrls; }

    public void submit() {
        this.status      = Status.SUBMITTED;
        this.submittedAt = Instant.now();
    }

    public void approve(UUID reviewedBy) {
        this.status     = Status.APPROVED;
        this.reviewedBy = reviewedBy;
        this.reviewedAt = Instant.now();
    }

    public void reject(UUID reviewedBy, String notes) {
        this.status      = Status.REJECTED;
        this.reviewedBy  = reviewedBy;
        this.reviewedAt  = Instant.now();
        this.reviewNotes = notes;
    }
}

