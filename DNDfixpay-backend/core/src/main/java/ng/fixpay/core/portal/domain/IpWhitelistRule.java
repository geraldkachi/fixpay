package ng.fixpay.core.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "ip_whitelist_rules")
public class IpWhitelistRule {

    public enum Environment { SANDBOX, LIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(name = "ip_cidr", nullable = false, length = 50)
    private String ipCidr;

    @Column(length = 120)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Environment environment;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    protected IpWhitelistRule() {}

    public IpWhitelistRule(UUID tenantId, String ipCidr, String label, Environment environment) {
        this.tenantId    = tenantId;
        this.ipCidr      = ipCidr;
        this.label       = label;
        this.environment = environment;
    }

    public UUID        getId()          { return id; }
    public UUID        getTenantId()    { return tenantId; }
    public String      getIpCidr()      { return ipCidr; }
    public String      getLabel()       { return label; }
    public Environment getEnvironment() { return environment; }
    public boolean     isActive()       { return active; }
    public Instant     getCreatedAt()   { return createdAt; }

    public void setActive(boolean active) { this.active = active; }
    public void setLabel(String label)    { this.label = label; }
}

