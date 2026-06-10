package ng.fixpay.core.mandate.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NibssMandateRepository extends JpaRepository<NibssMandate, UUID> {
    Optional<NibssMandate> findByMandateReference(String mandateReference);
    List<NibssMandate> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
