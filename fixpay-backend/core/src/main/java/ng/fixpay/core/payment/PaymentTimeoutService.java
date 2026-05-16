package ng.fixpay.core.payment;

import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.payment.domain.PaymentJournalEntry;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Scans for non-terminal payments that have exceeded the configured timeout window and
 * permanently fails them.
 *
 * <p>Design decisions:
 * <ul>
 *   <li>Each payment is processed in its own {@link TransactionTemplate} transaction so that a
 *       single bad row cannot roll back the whole batch.</li>
 *   <li>The reversal of a wallet debit ({@link LedgerService#reverseByUser}) runs in
 *       {@code REQUIRES_NEW} inside the per-payment transaction, so it is committed
 *       independently even if the outer transaction later fails.</li>
 *   <li>The timeout threshold is a {@code volatile} int in {@link PaymentTimeoutProperties} so
 *       the platform-admin API can change it at runtime without restarting the service.</li>
 * </ul>
 */
@Service
public class PaymentTimeoutService {

    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutService.class);

    private static final List<String> NON_TERMINAL_STATUSES =
            List.of("initiated", "authorized", "pending_authorization", "processing");

    /** Maximum payments processed in a single scheduler tick. */
    private static final int BATCH_SIZE = 100;

    private final VtpassPaymentRepository paymentRepository;
    private final PaymentJournalEntryRepository journalRepository;
    private final LedgerService ledgerService;
    private final DomainEventPublisher eventPublisher;
    private final PaymentTimeoutProperties properties;
    private final TransactionTemplate txTemplate;

    public PaymentTimeoutService(
            VtpassPaymentRepository paymentRepository,
            PaymentJournalEntryRepository journalRepository,
            LedgerService ledgerService,
            DomainEventPublisher eventPublisher,
            PaymentTimeoutProperties properties,
            PlatformTransactionManager txManager) {
        this.paymentRepository = paymentRepository;
        this.journalRepository  = journalRepository;
        this.ledgerService      = ledgerService;
        this.eventPublisher     = eventPublisher;
        this.properties         = properties;
        this.txTemplate         = new TransactionTemplate(txManager);
    }

    /**
     * Runs every 10 seconds (configurable via {@code fixpay.payment.timeout-check-interval-ms}).
     * Fetches up to {@value #BATCH_SIZE} non-terminal payments older than the timeout threshold
     * and permanently fails each one.
     */
    @Scheduled(fixedDelayString = "${fixpay.payment.timeout-check-interval-ms:10000}")
    public void expireTimedOutPayments() {
        int threshold = properties.getTimeoutSeconds();
        Instant cutoff = Instant.now().minusSeconds(threshold);

        List<VtpassPayment> expired = paymentRepository.findTimedOutPayments(
                NON_TERMINAL_STATUSES, cutoff, PageRequest.of(0, BATCH_SIZE));

        if (!expired.isEmpty()) {
            log.info("PaymentTimeoutService: found {} payment(s) to expire (threshold={}s)",
                    expired.size(), threshold);
        }

        for (VtpassPayment payment : expired) {
            try {
                expirePayment(payment.getId(), threshold);
            } catch (Exception e) {
                log.error("PaymentTimeoutService: error expiring payment {} — {}",
                        payment.getPaymentReference(), e.getMessage(), e);
            }
        }
    }

    // ─── Per-payment processing ───────────────────────────────────────────────

    void expirePayment(UUID paymentId, int threshold) {
        txTemplate.execute(status -> {
            VtpassPayment payment = paymentRepository.findById(paymentId).orElse(null);

            // Guard: skip if the payment was updated to a terminal state between the
            // batch query and now (e.g. a webhook arrived in the meantime).
            if (payment == null || payment.isInTerminalStatus()) {
                return null;
            }

            // Wallet debit reversal is only needed when a debit was previously committed.
            // This only happens for WALLET payments that reached the `processing` status.
            boolean needsWalletReversal = "processing".equals(payment.getPaymentStatus())
                    && payment.getPaymentMethod() == VtpassPaymentMethod.WALLET;

            payment.markTimedOut(threshold);
            paymentRepository.save(payment);

            journalRepository.save(new PaymentJournalEntry(
                    payment.getId(),
                    payment.getPaymentReference(),
                    "PAYMENT_TIMEOUT",
                    payment.getAmount(),
                    null, null,
                    "Payment timed out after " + threshold + "s — no provider response received",
                    null
            ));

            if (needsWalletReversal) {
                ledgerService.reverseByUser(
                        payment.getUserId(),
                        payment.getAmount(),
                        payment.getPaymentReference(),
                        "payment timeout after " + threshold + "s");

                journalRepository.save(new PaymentJournalEntry(
                        payment.getId(),
                        payment.getPaymentReference(),
                        "WALLET_REVERSED_TIMEOUT",
                        payment.getAmount(),
                        null, null,
                        "Wallet debit reversed because payment timed out",
                        null
                ));
            }

            eventPublisher.publish("payment.timeout", Map.of(
                    "paymentReference", payment.getPaymentReference(),
                    "paymentStatus",    payment.getPaymentStatus(),
                    "providerStatus",   payment.getProviderStatus(),
                    "paymentMethod",    payment.getPaymentMethod().name(),
                    "amount",           payment.getAmount().toPlainString(),
                    "tenantId",         payment.getTenantId().toString(),
                    "userId",           payment.getUserId().toString(),
                    "timeoutSeconds",   String.valueOf(threshold)
            ));

            log.warn("PaymentTimeoutService: expired payment {} (method={}, walletReversed={})",
                    payment.getPaymentReference(), payment.getPaymentMethod(), needsWalletReversal);

            return null;
        });
    }
}
