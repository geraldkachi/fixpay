package ng.fixpay.core.portal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ApiKeyRepository extends JpaRepository<ApiKey, UUID> {
    List<ApiKey> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<ApiKey> findByTenantIdAndEnvironmentOrderByCreatedAtDesc(UUID tenantId, ApiKey.Environment environment);
    Optional<ApiKey> findByKeyHash(String keyHash);
    boolean existsByTenantIdAndEnvironmentAndRevokedAtIsNull(UUID tenantId, ApiKey.Environment environment);
}

