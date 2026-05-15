package ng.fixpay.core.portal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SettlementAccountRepository extends JpaRepository<SettlementAccount, UUID> {
    Optional<SettlementAccount> findByTenantId(UUID tenantId);
}

