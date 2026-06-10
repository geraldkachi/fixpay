package ng.fixpay.core.dispute.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DisputeRepository extends JpaRepository<Dispute, UUID> {

    List<Dispute> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Dispute> findByIdAndUserId(UUID id, UUID userId);
}
