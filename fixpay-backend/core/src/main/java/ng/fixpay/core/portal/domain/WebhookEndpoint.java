package ng.fixpay.core.portal.domain;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "webhook_endpoints")
public class WebhookEndpoint {

    public enum Environment { SANDBOX, LIVE }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "tenant_id", nullable = false)
    private UUID tenantId;

    @Column(nullable = false, length = 500)
    private String url;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "webhook_endpoint_events", joinColumns = @JoinColumn(name = "webhook_id"))
    @Column(name = "event_type")
    private List<String> events;

    @Column(name = "signing_secret", nullable = false, length = 128)
    private String signingSecret;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Environment environment;

    @Column(nullable = false)
    private boolean active = true;

    @Column(name = "failure_count", nullable = false)
    private short failureCount = 0;

    @Column(name = "last_triggered")
    private Instant lastTriggered;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    protected WebhookEndpoint() {}

    public WebhookEndpoint(UUID tenantId, String url, List<String> events,
                           String signingSecret, Environment environment) {
        this.tenantId      = tenantId;
        this.url           = url;
        this.events        = events;
        this.signingSecret = signingSecret;
        this.environment   = environment;
    }

    @PreUpdate void onUpdate() { this.updatedAt = Instant.now(); }

    public UUID        getId()            { return id; }
    public UUID        getTenantId()      { return tenantId; }
    public String      getUrl()           { return url; }
    public List<String> getEvents()       { return events; }
    public String      getSigningSecret() { return signingSecret; }
    public Environment getEnvironment()   { return environment; }
    public boolean     isActive()         { return active; }
    public short       getFailureCount()  { return failureCount; }
    public Instant     getLastTriggered() { return lastTriggered; }
    public Instant     getCreatedAt()     { return createdAt; }
    public Instant     getUpdatedAt()     { return updatedAt; }

    public void setUrl(String url)             { this.url = url; }
    public void setEvents(List<String> events) { this.events = events; }
    public void setActive(boolean active)      { this.active = active; }
    public void recordTrigger()                { this.lastTriggered = Instant.now(); }
    public void incrementFailures()            { this.failureCount++; }
    public void resetFailures()                { this.failureCount = 0; }
}

