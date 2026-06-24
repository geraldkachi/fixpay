package ng.fixpay.core.tenant.domain;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;

@Entity
@Table(name = "tenants")
public class Tenant {

    public enum Status { SANDBOX, ACTIVE, SUSPENDED, OFFBOARDED }
    public enum Plan   { STARTER, GROWTH, ENTERPRISE }
    public enum KybStatus { PENDING, IN_REVIEW, APPROVED, REJECTED }

    @Id
    private UUID id;

    @Column(nullable = false, unique = true, length = 60)
    private String slug;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(name = "primary_color",   nullable = false, length = 9)
    private String primaryColor;

    @Column(name = "secondary_color", nullable = false, length = 9)
    private String secondaryColor;

    @Column(name = "accent_color",    nullable = false, length = 9)
    private String accentColor;

    @Column(name = "logo_url",    length = 500)
    private String logoUrl;

    @Column(name = "favicon_url", length = 500)
    private String faviconUrl;

    @Column(name = "support_email", length = 255)
    private String supportEmail;

    @Column(name = "support_phone", length = 20)
    private String supportPhone;

    @Column(name = "feat_bill_payments",      nullable = false) private boolean featBillPayments      = true;
    @Column(name = "feat_direct_debit",       nullable = false) private boolean featDirectDebit       = true;
    @Column(name = "feat_wallet_transfers",   nullable = false) private boolean featWalletTransfers   = true;
    @Column(name = "feat_intl_airtime",       nullable = false) private boolean featIntlAirtime       = false;
    @Column(name = "feat_dispute_management", nullable = false) private boolean featDisputeManagement = true;
    @Column(name = "feat_nibss_transfers",    nullable = false) private boolean featNibssTransfers    = true;

    @Column(nullable = false)
    private boolean active = true;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Plan plan = Plan.STARTER;

    @Enumerated(EnumType.STRING)
    @Column(name = "kyb_status", nullable = false, length = 20)
    private KybStatus kybStatus = KybStatus.PENDING;

    @Column(name = "suspended_at")
    private Instant suspendedAt;

    @Column(name = "suspended_reason", columnDefinition = "TEXT")
    private String suspendedReason;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "feature_flags", columnDefinition = "jsonb")
    private Map<String, Object> featureFlags = new HashMap<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "whitelabel_config", columnDefinition = "jsonb")
    private Map<String, Object> whitelabelConfig = new HashMap<>();

    @Column(name = "contact_email", length = 255)
    private String contactEmail;

    @Column(name = "go_live_requested_at")
    private Instant goLiveRequestedAt;

    @Column(name = "went_live_at")
    private Instant wentLiveAt;

    @Column(name = "sandbox_wallet_balance", nullable = false)
    private long sandboxWalletBalance = 10_000_000L; // ₦100,000 in kobo

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected Tenant() {}

    /** Factory for self-service portal registration. */
    public static Tenant selfRegister(UUID id, String slug, String name, String contactEmail) {
        Tenant t = new Tenant();
        t.id           = id;
        t.slug         = slug;
        t.name         = name;
        t.contactEmail = contactEmail;
        t.status       = Status.SANDBOX;
        t.kybStatus    = KybStatus.PENDING;
        t.plan         = Plan.STARTER;
        t.active       = true;
        t.primaryColor   = "#A51D21";
        t.secondaryColor = "#34C759";
        t.accentColor    = "#FF9500";
        return t;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ─── Getters ────────────────────────────────────────────────────────────

    public UUID    getId()              { return id; }
    public String  getSlug()            { return slug; }
    public String  getName()            { return name; }
    public String  getPrimaryColor()    { return primaryColor; }
    public String  getSecondaryColor()  { return secondaryColor; }
    public String  getAccentColor()     { return accentColor; }
    public String  getLogoUrl()         { return logoUrl; }
    public String  getFaviconUrl()      { return faviconUrl; }
    public String  getSupportEmail()    { return supportEmail; }
    public String  getSupportPhone()    { return supportPhone; }
    public boolean isFeatBillPayments()      { return featBillPayments; }
    public boolean isFeatDirectDebit()       { return featDirectDebit; }
    public boolean isFeatWalletTransfers()   { return featWalletTransfers; }
    public boolean isFeatIntlAirtime()       { return featIntlAirtime; }
    public boolean isFeatDisputeManagement() { return featDisputeManagement; }
    public boolean isFeatNibssTransfers()    { return featNibssTransfers; }
    public boolean isActive()           { return active; }
    public Status    getStatus()          { return status; }
    public Plan      getPlan()            { return plan; }
    public KybStatus getKybStatus()       { return kybStatus; }
    public Instant   getSuspendedAt()     { return suspendedAt; }
    public String    getSuspendedReason() { return suspendedReason; }
    public Map<String, Object> getFeatureFlags()      { return featureFlags; }
    public Map<String, Object> getWhitelabelConfig()  { return whitelabelConfig; }
    public String  getContactEmail()        { return contactEmail; }
    public Instant getGoLiveRequestedAt()   { return goLiveRequestedAt; }
    public Instant getWentLiveAt()          { return wentLiveAt; }
    public long    getSandboxWalletBalance(){ return sandboxWalletBalance; }
    public Instant getCreatedAt()           { return createdAt; }
    public Instant getUpdatedAt()           { return updatedAt; }

    // ─── Setters for admin operations ───────────────────────────────────────

    public void setName(String name)         { this.name = name; }
    public void setActive(boolean active)    { this.active = active; }
    public void setStatus(Status status)     { this.status = status; }
    public void setPlan(Plan plan)           { this.plan = plan; }
    public void setKybStatus(KybStatus kybStatus) { this.kybStatus = kybStatus; }
    public void setSuspendedAt(Instant suspendedAt)         { this.suspendedAt = suspendedAt; }
    public void setSuspendedReason(String suspendedReason)  { this.suspendedReason = suspendedReason; }
    public void setFeatureFlags(Map<String, Object> featureFlags)       { this.featureFlags = featureFlags; }
    public void setWhitelabelConfig(Map<String, Object> whitelabelConfig) { this.whitelabelConfig = whitelabelConfig; }
    public void setSupportEmail(String supportEmail)  { this.supportEmail = supportEmail; }
    public void setSupportPhone(String supportPhone)  { this.supportPhone = supportPhone; }
    public void setContactEmail(String contactEmail)          { this.contactEmail = contactEmail; }
    public void setGoLiveRequestedAt(Instant goLiveRequestedAt) { this.goLiveRequestedAt = goLiveRequestedAt; }
    public void setWentLiveAt(Instant wentLiveAt)              { this.wentLiveAt = wentLiveAt; }
    public void setPrimaryColor(String primaryColor)           { this.primaryColor = primaryColor; }
    public void setSecondaryColor(String secondaryColor)       { this.secondaryColor = secondaryColor; }
    public void setAccentColor(String accentColor)             { this.accentColor = accentColor; }
    public void setLogoUrl(String logoUrl)                     { this.logoUrl = logoUrl; }
    public void setFaviconUrl(String faviconUrl)               { this.faviconUrl = faviconUrl; }
}
