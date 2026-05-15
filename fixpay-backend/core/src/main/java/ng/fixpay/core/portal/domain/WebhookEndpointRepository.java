package ng.fixpay.core.portal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface WebhookEndpointRepository extends JpaRepository<WebhookEndpoint, UUID> {
    List<WebhookEndpoint> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<WebhookEndpoint> findByTenantIdAndEnvironment(UUID tenantId, WebhookEndpoint.Environment environment);
}

