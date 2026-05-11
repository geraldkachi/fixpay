package ng.fixpay.core.kyc.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface KycVerificationRepository extends JpaRepository<KycVerification, UUID> {
    Optional<KycVerification> findTopByUserIdOrderByCreatedAtDesc(UUID userId);
}
