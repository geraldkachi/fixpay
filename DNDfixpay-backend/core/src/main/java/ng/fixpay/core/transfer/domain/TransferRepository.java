package ng.fixpay.core.transfer.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TransferRepository extends JpaRepository<Transfer, UUID> {

    List<Transfer> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Transfer> findByReference(String reference);
}
