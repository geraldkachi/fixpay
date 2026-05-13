package ng.fixpay.core.ledger.domain;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    List<LedgerEntry> findByWalletIdOrderByCreatedAtAsc(UUID walletId);

    List<LedgerEntry> findByCorrelationIdOrderByCreatedAtAsc(String correlationId);

    List<LedgerEntry> findByReferenceOrderByCreatedAtAsc(String reference);

    /** Most recent ledger entry for a wallet — gives the current running balance. */
    Optional<LedgerEntry> findFirstByWalletIdOrderByCreatedAtDesc(UUID walletId);

    @Query("SELECT COALESCE(SUM(CASE WHEN e.entryType = 'CREDIT' THEN e.amount ELSE -e.amount END), 0) " +
           "FROM LedgerEntry e WHERE e.walletId = :walletId AND e.currency = :currency")
    BigDecimal computeBalance(UUID walletId, String currency);
}
