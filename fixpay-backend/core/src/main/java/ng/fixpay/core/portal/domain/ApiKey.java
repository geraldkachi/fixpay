package ng.fixpay.core.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "api_keys")
public class ApiKey {

    public enum Environment { SANDBOX, LIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Environment environment;

    @Column(name = "key_prefix", nullable = false, length = 20)
    private String keyPrefix;

    @Column(name = "key_hash", nullable = false, unique = true, length = 64)
    private String keyHash;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "api_key_scopes", joinColumns = @JoinColumn(name = "api_key_id"))
    @Column(name = "scope")
    private List<String> scopes;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected ApiKey() {}

    public ApiKey(UUID tenantId, String name, Environment environment,
                  String keyPrefix, String keyHash, List<String> scopes, UUID createdBy) {
        this.tenantId    = tenantId;
        this.name        = name;
        this.environment = environment;
        this.keyPrefix   = keyPrefix;
        this.keyHash     = keyHash;
        this.scopes      = scopes;
        this.createdBy   = createdBy;
    }

    public UUID        getId()          { return id; }
    public UUID        getTenantId()    { return tenantId; }
    public String      getName()        { return name; }
    public Environment getEnvironment() { return environment; }
    public String      getKeyPrefix()   { return keyPrefix; }
    public String      getKeyHash()     { return keyHash; }
    public List<String> getScopes()     { return scopes; }
    public Instant     getLastUsedAt()  { return lastUsedAt; }
    public Instant     getExpiresAt()   { return expiresAt; }
    public Instant     getRevokedAt()   { return revokedAt; }
    public UUID        getCreatedBy()   { return createdBy; }
    public Instant     getCreatedAt()   { return createdAt; }

    public boolean isRevoked()          { return revokedAt != null; }
    public boolean isExpired()          { return expiresAt != null && Instant.now().isAfter(expiresAt); }
    public boolean isActive()           { return !isRevoked() && !isExpired(); }

    public void revoke()                { this.revokedAt = Instant.now(); }
    public void markUsed()              { this.lastUsedAt = Instant.now(); }
}

