package ng.fixpay.core.portal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface IpWhitelistRuleRepository extends JpaRepository<IpWhitelistRule, UUID> {
    List<IpWhitelistRule> findByTenantIdOrderByCreatedAtDesc(UUID tenantId);
    List<IpWhitelistRule> findByTenantIdAndEnvironmentAndActiveTrue(UUID tenantId, IpWhitelistRule.Environment environment);
}

