package ng.fixpay.core.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VtpassPaymentRepository extends JpaRepository<VtpassPayment, UUID> {
    Optional<VtpassPayment> findByPaymentReference(String paymentReference);
}
