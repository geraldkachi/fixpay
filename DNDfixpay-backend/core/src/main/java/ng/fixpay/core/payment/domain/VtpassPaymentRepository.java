package ng.fixpay.core.payment.domain;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VtpassPaymentRepository extends JpaRepository<VtpassPayment, UUID> {
    Optional<VtpassPayment> findByPaymentReference(String paymentReference);
    Optional<VtpassPayment> findByInitIdempotencyKeyAndUserId(String initIdempotencyKey, UUID userId);

    @Query("SELECT p FROM VtpassPayment p WHERE p.paymentStatus IN :statuses AND p.createdAt < :cutoff")
    List<VtpassPayment> findTimedOutPayments(
            @Param("statuses") Collection<String> statuses,
            @Param("cutoff") Instant cutoff,
            Pageable pageable);
}
