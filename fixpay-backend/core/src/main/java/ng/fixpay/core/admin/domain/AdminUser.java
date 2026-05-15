package ng.fixpay.core.admin.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "admin_users")
public class AdminUser {

    public enum AdminRole {
        PLATFORM_ADMIN, SUPPORT_AGENT, COMPLIANCE_OFFICER, FINANCE_OPS
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "keycloak_user_id", nullable = false, unique = true, length = 255)
    private String keycloakUserId;

    @Column(nullable = false, length = 100)
    private String username;

    @Column(nullable = false, length = 255)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private AdminRole role;

    /** NULL means the user has access to all tenants */
    @Column(name = "tenant_scope")
    private UUID tenantScope;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "last_login_at")
    private Instant lastLoginAt;

    protected AdminUser() {}

    public AdminUser(String keycloakUserId, String username, String email, AdminRole role, UUID tenantScope) {
        this.keycloakUserId = keycloakUserId;
        this.username       = username;
        this.email          = email;
        this.role           = role;
        this.tenantScope    = tenantScope;
    }

    public UUID      getId()             { return id; }
    public String    getKeycloakUserId() { return keycloakUserId; }
    public String    getUsername()       { return username; }
    public String    getEmail()          { return email; }
    public AdminRole getRole()           { return role; }
    public UUID      getTenantScope()    { return tenantScope; }
    public boolean   isActive()          { return isActive; }
    public Instant   getCreatedAt()      { return createdAt; }
    public Instant   getLastLoginAt()    { return lastLoginAt; }

    public void setRole(AdminRole role)           { this.role = role; }
    public void setActive(boolean active)          { this.isActive = active; }
    public void setLastLoginAt(Instant lastLoginAt) { this.lastLoginAt = lastLoginAt; }
    public void setTenantScope(UUID tenantScope)   { this.tenantScope = tenantScope; }
}
