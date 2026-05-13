package ng.fixpay.core.ledger;

import ng.fixpay.core.ledger.domain.LedgerEntry;
import ng.fixpay.core.ledger.domain.LedgerEntry.EntryType;
import ng.fixpay.core.ledger.domain.LedgerEntryRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Single source of truth for all wallet balance mutations.
 *
 * <p>Every balance change is recorded as a pair of immutable {@link LedgerEntry} rows
 * (double-entry accounting). The wallet's {@code balance} column is kept in sync as a
 * denormalised read cache only — the ledger is the book of record.
 *
 * <p>Rules:
 * <ul>
 *   <li>No other service may call {@code wallet.setBalance()} directly.</li>
 *   <li>All ledger methods run in {@code REQUIRES_NEW} so that a reversal can
 *       succeed even if the surrounding transaction rolls back (prevents ghost debits).</li>
 *   <li>Running balance is stored on the DEBIT leg; the CREDIT leg mirrors it.</li>
 * </ul>
 */
@Service
public class LedgerService {

    private final WalletRepository walletRepository;
    private final LedgerEntryRepository ledgerRepository;

    public LedgerService(WalletRepository walletRepository, LedgerEntryRepository ledgerRepository) {
        this.walletRepository  = walletRepository;
        this.ledgerRepository  = ledgerRepository;
    }

    // ─── Public API ──────────────────────────────────────────────────────────

    /**
     * Debits the user's NGN wallet.
     *
     * <p>Records two ledger rows in the same database flush:
     * <ol>
     *   <li>DEBIT on the user wallet — reduces available balance.</li>
     *   <li>CREDIT on the system "payments payable" account — the money is now owed to the provider.</li>
     * </ol>
     *
     * @param userId      wallet owner
     * @param amount      positive amount to debit
     * @param reference   payment reference
     * @param description human-readable reason (e.g. "Wallet debit for VTpass airtime")
     * @return snapshot of balances before and after for audit use
     * @throws FixPayException 400 if balance is insufficient, 404 if wallet not found
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DebitResult debit(UUID userId, BigDecimal amount, String reference, String description) {
        Wallet wallet = requireWallet(userId);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw FixPayException.badRequest("Insufficient wallet balance");
        }

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.subtract(amount);
        String correlationId = reference + "-DEBIT";

        wallet.setBalance(balanceAfter);
        wallet.setLedgerBalance(wallet.getLedgerBalance().subtract(amount));

        // DEBIT leg — user wallet loses funds
        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), userId, wallet.getTenantId(),
                correlationId, EntryType.DEBIT,
                amount, balanceAfter, "NGN", reference, description
        ));

        // CREDIT leg — system "payments payable" account gains funds (same wallet context)
        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), userId, wallet.getTenantId(),
                correlationId, EntryType.CREDIT,
                amount, balanceAfter, "NGN", reference, "Payments payable — " + description
        ));

        return new DebitResult(wallet.getId(), userId, balanceBefore, balanceAfter, amount, correlationId);
    }

    /**
     * Reverses a previous debit — restores the wallet balance.
     *
     * <p>Called when a provider call fails after a debit has been committed to a
     * separate transaction (REQUIRES_NEW). Writing the reversal in yet another
     * REQUIRES_NEW transaction ensures the credit is persisted even if the
     * outer orchestration transaction rolls back.
     *
     * @param debitResult the result returned from the corresponding {@link #debit} call
     * @param reason      why the reversal is happening
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void reverse(DebitResult debitResult, String reference, String reason) {
        Wallet wallet = walletRepository.findById(debitResult.walletId())
                .orElseThrow(() -> FixPayException.notFound("Wallet for reversal"));

        BigDecimal reversedBalance = wallet.getBalance().add(debitResult.amount());
        String correlationId = reference + "-REVERSAL";

        wallet.setBalance(reversedBalance);
        wallet.setLedgerBalance(wallet.getLedgerBalance().add(debitResult.amount()));

        UUID userId = debitResult.userId() != null ? debitResult.userId() : wallet.getUserId();

        // CREDIT leg — returns funds to user wallet
        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), debitResult.userId(), wallet.getTenantId(),
                correlationId, EntryType.CREDIT,
                debitResult.amount(), reversedBalance, "NGN", reference, "Reversal: " + reason
        ));

        // DEBIT leg — removes from "payments payable"
        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), debitResult.userId(), wallet.getTenantId(),
                correlationId, EntryType.DEBIT,
                debitResult.amount(), reversedBalance, "NGN", reference, "Payments payable reversal: " + reason
        ));
    }

    /**
     * Credits the wallet (e.g. funding via bank transfer).
     *
     * @param userId      wallet owner
     * @param amount      positive amount to credit
     * @param reference   funding reference
     * @param description human-readable reason
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void credit(UUID userId, BigDecimal amount, String reference, String description) {
        Wallet wallet = requireWallet(userId);

        BigDecimal balanceBefore = wallet.getBalance();
        BigDecimal balanceAfter  = balanceBefore.add(amount);
        String correlationId = reference + "-CREDIT";

        wallet.setBalance(balanceAfter);
        wallet.setLedgerBalance(wallet.getLedgerBalance().add(amount));

        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), userId, wallet.getTenantId(),
                correlationId, EntryType.CREDIT,
                amount, balanceAfter, "NGN", reference, description
        ));
        ledgerRepository.save(new LedgerEntry(
                wallet.getId(), userId, wallet.getTenantId(),
                correlationId, EntryType.DEBIT,
                amount, balanceAfter, "NGN", reference, "Funding source — " + description
        ));
    }

    // ─── Internal helpers ─────────────────────────────────────────────────────

    private Wallet requireWallet(UUID userId) {
        return walletRepository.findByUserIdAndCurrency(userId, "NGN")
                .orElseThrow(() -> FixPayException.notFound("Wallet"));
    }

    // ─── Value objects ───────────────────────────────────────────────────────

    /**
     * Snapshot returned after a successful debit, passed to {@link #reverse} if needed.
     */
    public record DebitResult(
            UUID      walletId,
            UUID      userId,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            BigDecimal amount,
            String    correlationId
    ) {}
}
