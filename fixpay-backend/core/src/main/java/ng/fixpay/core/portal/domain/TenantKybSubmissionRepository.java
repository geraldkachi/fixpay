package ng.fixpay.core.portal.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TenantKybSubmissionRepository extends JpaRepository<TenantKybSubmission, UUID> {
    Optional<TenantKybSubmission> findByTenantId(UUID tenantId);
    List<TenantKybSubmission> findByStatusOrderBySubmittedAtAsc(TenantKybSubmission.Status status);
}

