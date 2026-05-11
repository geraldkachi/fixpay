package ng.fixpay.core.user.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
public class AppUser {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_id", nullable = false, unique = true)
    private UUID keycloakId;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, unique = true, length = 20)
    private String phone;

    @Column(length = 255)
    private String email;

    @Column(name = "kyc_status", nullable = false, length = 20)
    private String kycStatus = "pending";

    @Column(nullable = false)
    private short tier = 1;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected AppUser() {}

    public AppUser(UUID keycloakId, UUID tenantId, String phone, String email) {
        this.keycloakId = keycloakId;
        this.tenantId   = tenantId;
        this.phone      = phone;
        this.email      = email;
    }

    @PreUpdate
    void onUpdate() { this.updatedAt = Instant.now(); }

    // ─── Getters ────────────────────────────────────────────────────────────

    public UUID getId()         { return id; }
    public UUID getKeycloakId() { return keycloakId; }
    public UUID getTenantId()   { return tenantId; }
    public String getPhone()    { return phone; }
    public String getEmail()    { return email; }
    public String getKycStatus(){ return kycStatus; }
    public short getTier()      { return tier; }
    public Instant getCreatedAt(){ return createdAt; }
    public Instant getUpdatedAt(){ return updatedAt; }

    public void setKycStatus(String kycStatus) { this.kycStatus = kycStatus; }
    public void setTier(short tier)            { this.tier = tier; }
    public void setEmail(String email)         { this.email = email; }
}
