package ng.fixpay.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for VtpassPaymentService webhook processing.
 *
 * Validates:
 *  - HMAC-SHA256 signature verification (valid, invalid, null, without prefix, with sha256= prefix)
 *  - State transitions driven by provider status: delivered → completed, processing → processing, failed → failed
 *
 * No Spring context — no Keycloak, PostgreSQL, or Redis required.
 */
@ExtendWith(MockitoExtension.class)
class VtpassPaymentServiceWebhookTest {

    @Mock UserRepository userRepository;
    @Mock VtpassPaymentRepository paymentRepository;
    @Mock PaymentJournalEntryRepository journalRepository;
    @Mock WalletRepository walletRepository;
    @Mock VtpassClient vtpassClient;
    @Mock MandateService mandateService;
    @Mock DomainEventPublisher eventPublisher;

    private static final String WEBHOOK_SECRET = "test_webhook_secret_for_unit_tests";

    private VtpassPaymentService service;

    @BeforeEach
    void setUp() {
        service = new VtpassPaymentService(
            userRepository,
            paymentRepository,
            journalRepository,
            walletRepository,
            vtpassClient,
            new ObjectMapper(),
            mandateService,
            eventPublisher,
            WEBHOOK_SECRET
        );
    }

    // ─────────────────────────────────────────────────────────────────────────
    // State transitions
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void processWebhook_deliveredStatus_shouldMarkPaymentCompleted() throws Exception {
        String payload = buildPayload("FP-VTP-001", "delivered", "000", "Success", "PROV-001");
        String sig = "sha256=" + computeHmac(WEBHOOK_SECRET, payload);

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-001");
        when(paymentRepository.findByPaymentReference("FP-VTP-001")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(sig, payload);

        assertEquals("completed", payment.getPaymentStatus());
        assertEquals("delivered",  payment.getProviderStatus());
        assertEquals("000",        payment.getProviderCode());
        assertEquals("PROV-001",   payment.getExternalReference());
    }

    @Test
    void processWebhook_completedAlias_shouldMarkPaymentCompleted() throws Exception {
        String payload = buildPayload("FP-VTP-002", "completed", "000", "Success", "PROV-002");
        String sig = "sha256=" + computeHmac(WEBHOOK_SECRET, payload);

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-002");
        when(paymentRepository.findByPaymentReference("FP-VTP-002")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(sig, payload);

        assertEquals("completed", payment.getPaymentStatus());
    }

    @Test
    void processWebhook_processingStatus_shouldMarkPaymentProcessing() throws Exception {
        String payload = buildPayload("FP-VTP-003", "processing", "099", "Processing", "PROV-003");
        String sig = "sha256=" + computeHmac(WEBHOOK_SECRET, payload);

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-003");
        when(paymentRepository.findByPaymentReference("FP-VTP-003")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(sig, payload);

        assertEquals("processing", payment.getPaymentStatus());
        assertEquals("processing", payment.getProviderStatus());
    }

    @Test
    void processWebhook_pendingAlias_shouldMarkPaymentProcessing() throws Exception {
        String payload = buildPayload("FP-VTP-004", "pending", "099", "Pending", "PROV-004");
        String sig = "sha256=" + computeHmac(WEBHOOK_SECRET, payload);

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-004");
        when(paymentRepository.findByPaymentReference("FP-VTP-004")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(sig, payload);

        assertEquals("processing", payment.getPaymentStatus());
    }

    @Test
    void processWebhook_failedStatus_shouldMarkPaymentFailed() throws Exception {
        String payload = buildPayload("FP-VTP-005", "failed", "099", "Transaction failed", "PROV-005");
        String sig = "sha256=" + computeHmac(WEBHOOK_SECRET, payload);

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-005");
        when(paymentRepository.findByPaymentReference("FP-VTP-005")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(sig, payload);

        assertEquals("failed",  payment.getPaymentStatus());
        assertEquals("failed",  payment.getProviderStatus());
        assertEquals("099",     payment.getProviderCode());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // HMAC signature validation
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    void processWebhook_invalidSignature_shouldThrow403() {
        String payload = buildPayload("FP-VTP-001", "delivered", "000", "Success", "PROV-001");

        FixPayException ex = assertThrows(FixPayException.class,
            () -> service.processWebhook("sha256=completelywronghash", payload));

        assertEquals(403, ex.getHttpStatus());
        assertEquals("FORBIDDEN", ex.getErrorCode());
    }

    @Test
    void processWebhook_nullSignature_shouldThrow403() {
        String payload = buildPayload("FP-VTP-001", "delivered", "000", "Success", "PROV-001");

        FixPayException ex = assertThrows(FixPayException.class,
            () -> service.processWebhook(null, payload));

        assertEquals(403, ex.getHttpStatus());
    }

    @Test
    void processWebhook_signatureWithoutPrefix_shouldAlsoBeAccepted() throws Exception {
        // The service strips the "sha256=" prefix when present, but also accepts the raw hex
        String payload = buildPayload("FP-VTP-006", "delivered", "000", "Success", "PROV-006");
        String rawHex = computeHmac(WEBHOOK_SECRET, payload); // no "sha256=" prefix

        VtpassPayment payment = buildAuthorizedPayment("FP-VTP-006");
        when(paymentRepository.findByPaymentReference("FP-VTP-006")).thenReturn(Optional.of(payment));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.processWebhook(rawHex, payload);

        assertEquals("completed", payment.getPaymentStatus());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────────

    private static VtpassPayment buildAuthorizedPayment(String ref) {
        VtpassPayment payment = new VtpassPayment(
            UUID.randomUUID(),
            UUID.randomUUID(),
            ref,
            "airtel-airtime",
            "08011111111",
            new BigDecimal("100"),
            VtpassPaymentMethod.WALLET,
            null,
            UUID.randomUUID().toString()
        );
        payment.markAuthorized("pending", null, null);
        return payment;
    }

    private static String buildPayload(
            String paymentRef,
            String providerStatus,
            String providerCode,
            String providerMessage,
            String providerRequestId) {
        return String.format(
            "{\"paymentReference\":\"%s\",\"providerStatus\":\"%s\"," +
            "\"providerCode\":\"%s\",\"providerMessage\":\"%s\",\"providerRequestId\":\"%s\"}",
            paymentRef, providerStatus, providerCode, providerMessage, providerRequestId
        );
    }

    private static String computeHmac(String secret, String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder(digest.length * 2);
        for (byte b : digest) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
