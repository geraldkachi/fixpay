package ng.fixpay.core.ledger.domain;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
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

    /** Cross-tenant paginated ledger for admin with optional filters. */
    @Query("SELECT e FROM LedgerEntry e WHERE " +
           "(:tenantId IS NULL OR e.tenantId = :tenantId) AND " +
           "(:userId IS NULL OR e.userId = :userId) AND " +
           "(:entryType IS NULL OR e.entryType = :entryType) AND " +
           "(:fromDate IS NULL OR e.createdAt >= :fromDate) AND " +
           "(:toDate   IS NULL OR e.createdAt <= :toDate) AND " +
           "(:reference IS NULL OR LOWER(e.reference) LIKE LOWER(CONCAT('%', :reference, '%')))")
    Page<LedgerEntry> adminSearch(
            @Param("tenantId")   UUID tenantId,
            @Param("userId")     UUID userId,
            @Param("entryType")  LedgerEntry.EntryType entryType,
            @Param("fromDate")   Instant fromDate,
            @Param("toDate")     Instant toDate,
            @Param("reference")  String reference,
            Pageable pageable);
}

