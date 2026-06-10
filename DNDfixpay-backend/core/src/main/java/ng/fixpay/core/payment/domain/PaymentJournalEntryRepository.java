package ng.fixpay.core.payment.domain;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentJournalEntryRepository extends JpaRepository<PaymentJournalEntry, UUID> {
    List<PaymentJournalEntry> findByPaymentReferenceOrderByCreatedAtAsc(String paymentReference);
}
