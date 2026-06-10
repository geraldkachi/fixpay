package ng.fixpay.core.payment;

import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.payment.domain.PaymentJournalEntry;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionStatus;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link PaymentTimeoutService}.
 *
 * <p>Key scenarios verified:
 * <ul>
 *   <li>WALLET + {@code processing} payment is failed AND its wallet debit is reversed.</li>
 *   <li>WALLET + {@code authorized} payment is failed WITHOUT wallet reversal (debit not yet made).</li>
 *   <li>Non-WALLET {@code processing} payment is failed WITHOUT wallet reversal.</li>
 *   <li>A payment already in terminal state is skipped (idempotency guard).</li>
 *   <li>When there are no expired payments the batch loop runs zero iterations.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class PaymentTimeoutServiceTest {

    @Mock VtpassPaymentRepository paymentRepository;
    @Mock PaymentJournalEntryRepository journalRepository;
    @Mock LedgerService ledgerService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock PlatformTransactionManager txManager;

    private PaymentTimeoutProperties properties;
    private PaymentTimeoutService service;

    private static final UUID USER_ID   = UUID.randomUUID();
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final int  TIMEOUT   = 60;

    @BeforeEach
    void setUp() {
        // Make TransactionTemplate execute the callback synchronously (no real DB transaction).
        lenient().when(txManager.getTransaction(any())).thenReturn(mock(TransactionStatus.class));

        properties = new PaymentTimeoutProperties();
        properties.setTimeoutSeconds(TIMEOUT);

        service = new PaymentTimeoutService(
                paymentRepository, journalRepository, ledgerService, eventPublisher,
                properties, txManager);

        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ── expirePayment: WALLET + processing ───────────────────────────────────

    @Test
    void expirePayment_walletProcessingPayment_shouldFailAndReverseWalletDebit() {
        UUID paymentId = UUID.randomUUID();
        VtpassPayment payment = buildPayment("FP-TIMEOUT-01", VtpassPaymentMethod.WALLET, paymentId);
        payment.markProcessing("pending", null, null, null);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.expirePayment(paymentId, TIMEOUT);

        verify(paymentRepository).save(argThat(p -> "failed".equals(p.getPaymentStatus())));
        verify(journalRepository, atLeast(2)).save(argThat(e ->
                "PAYMENT_TIMEOUT".equals(e.getEventType()) || "WALLET_REVERSED_TIMEOUT".equals(e.getEventType())));
        verify(ledgerService).reverseByUser(eq(USER_ID), eq(new BigDecimal("500.00")),
                eq("FP-TIMEOUT-01"), contains("timeout"));
        verify(eventPublisher).publish(eq("payment.timeout"), anyMap());
    }

    // ── expirePayment: WALLET + authorized (no debit yet) ────────────────────

    @Test
    void expirePayment_walletAuthorizedPayment_shouldFailWithoutWalletReversal() {
        UUID paymentId = UUID.randomUUID();
        VtpassPayment payment = buildPayment("FP-TIMEOUT-02", VtpassPaymentMethod.WALLET, paymentId);
        payment.markAuthorized("authorized", null, null);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.expirePayment(paymentId, TIMEOUT);

        verify(paymentRepository).save(argThat(p -> "failed".equals(p.getPaymentStatus())));
        verify(journalRepository).save(argThat(e -> "PAYMENT_TIMEOUT".equals(e.getEventType())));
        verify(ledgerService, never()).reverseByUser(any(), any(), any(), any());
        verify(eventPublisher).publish(eq("payment.timeout"), anyMap());
    }

    // ── expirePayment: non-WALLET processing (bank/card) ─────────────────────

    @Test
    void expirePayment_nonWalletProcessingPayment_shouldFailWithoutWalletReversal() {
        UUID paymentId = UUID.randomUUID();
        VtpassPayment payment = buildPayment("FP-TIMEOUT-03", VtpassPaymentMethod.BANK_TRANSFER, paymentId);
        payment.markProcessing("pending", null, null, null);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.expirePayment(paymentId, TIMEOUT);

        verify(paymentRepository).save(argThat(p -> "failed".equals(p.getPaymentStatus())));
        verify(ledgerService, never()).reverseByUser(any(), any(), any(), any());
        verify(eventPublisher).publish(eq("payment.timeout"), anyMap());
    }

    // ── expirePayment: already-failed payment is skipped ─────────────────────

    @Test
    void expirePayment_alreadyFailedPayment_shouldSkipProcessing() {
        UUID paymentId = UUID.randomUUID();
        VtpassPayment payment = buildPayment("FP-TIMEOUT-04", VtpassPaymentMethod.WALLET, paymentId);
        payment.markFailed("error", "ERR", "some earlier failure", null);
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.expirePayment(paymentId, TIMEOUT);

        verify(paymentRepository, never()).save(any());
        verify(ledgerService, never()).reverseByUser(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    // ── expireTimedOutPayments: empty batch ──────────────────────────────────

    @Test
    void expireTimedOutPayments_noExpiredPayments_shouldDoNothing() {
        when(paymentRepository.findTimedOutPayments(any(), any(), any()))
                .thenReturn(List.of());

        service.expireTimedOutPayments();

        verify(paymentRepository, never()).findById(any());
        verify(ledgerService, never()).reverseByUser(any(), any(), any(), any());
        verify(eventPublisher, never()).publish(any(), any());
    }

    // ── expireTimedOutPayments: batch with one payment ────────────────────────

    @Test
    void expireTimedOutPayments_withOneBatchPayment_shouldDelegateToExpirePayment() {
        UUID paymentId = UUID.randomUUID();
        VtpassPayment payment = buildPayment("FP-TIMEOUT-05", VtpassPaymentMethod.WALLET, paymentId);
        payment.markAuthorized("authorized", null, null);

        when(paymentRepository.findTimedOutPayments(any(), any(), any()))
                .thenReturn(List.of(payment));
        when(paymentRepository.findById(paymentId)).thenReturn(Optional.of(payment));

        service.expireTimedOutPayments();

        verify(paymentRepository).save(argThat(p -> "failed".equals(p.getPaymentStatus())));
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private VtpassPayment buildPayment(String ref, VtpassPaymentMethod method, UUID id) {
        VtpassPayment payment = new VtpassPayment(
                USER_ID, TENANT_ID, ref, "airtel-airtime", "08011111111",
                new BigDecimal("500.00"), method, null, null);
        setId(payment, id);
        return payment;
    }

    private static void setId(VtpassPayment payment, UUID id) {
        try {
            Field f = VtpassPayment.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(payment, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
