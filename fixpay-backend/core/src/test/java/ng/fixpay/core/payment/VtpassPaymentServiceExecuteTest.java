package ng.fixpay.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.ledger.LedgerService.DebitResult;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.payment.provider.VtpassPurchaseResult;
import ng.fixpay.core.payment.provider.VtpassServiceRegistry;
import ng.fixpay.core.payment.rail.FeeCalculatorService;
import ng.fixpay.core.payment.rail.PaymentRailRegistry;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VtpassPaymentService#execute}.
 *
 * <p>Validates the second-step execution path of the multi-rail payment flow:
 * <ul>
 *   <li>WALLET rail debits the ledger on successful purchase</li>
 *   <li>WALLET rail reverses the debit and publishes a failed event when the provider fails</li>
 *   <li>Duplicate execute idempotency key short-circuits before any debit or provider call</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VtpassPaymentServiceExecuteTest {

    @Mock UserRepository userRepository;
    @Mock VtpassPaymentRepository paymentRepository;
    @Mock PaymentJournalEntryRepository journalRepository;
    @Mock LedgerService ledgerService;
    @Mock VtpassClient vtpassClient;
    @Mock VtpassServiceRegistry serviceRegistry;
    @Mock PaymentRailRegistry railRegistry;
    @Mock FeeCalculatorService feeCalculatorService;
    @Mock PaymentRailAdapter paymentRailAdapter;
    @Mock MandateService mandateService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock Jwt jwt;

    private VtpassPaymentService service;

    private static final UUID KEYCLOAK_ID = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID TENANT_ID   = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new VtpassPaymentService(
            userRepository, paymentRepository, journalRepository, ledgerService,
            vtpassClient, serviceRegistry, railRegistry, feeCalculatorService,
            new ObjectMapper(), mandateService, eventPublisher, "test_webhook_secret"
        );
        lenient().when(jwt.getSubject()).thenReturn(KEYCLOAK_ID.toString());
        lenient().when(paymentRailAdapter.processorId()).thenReturn("wallet");
        lenient().when(railRegistry.resolveById("wallet")).thenReturn(Optional.of(paymentRailAdapter));
        lenient().when(paymentRailAdapter.confirmFunded(any()))
                .thenReturn(PaymentRailResult.funded("RAIL-REF", null));
        lenient().when(ledgerService.debit(any(), any(), any(), any()))
                .thenReturn(new DebitResult(UUID.randomUUID(), USER_ID,
                        new BigDecimal("5000.00"), new BigDecimal("4500.00"),
                        new BigDecimal("500.00"), "corr-1"));
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    // ─── Wallet debit ─────────────────────────────────────────────────────────

    @Test
    void execute_walletRail_success_shouldDebitWalletOnce() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-EXEC-01");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-EXEC-01")).thenReturn(Optional.of(payment));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successResult("REQ-001"));

        service.execute(jwt, "FP-VTP-EXEC-01", null);

        verify(ledgerService, times(1)).debit(eq(USER_ID), eq(new BigDecimal("500.00")), anyString(), anyString());
        verify(ledgerService, never()).reverse(any(), anyString(), anyString());
    }

    @Test
    void execute_walletRail_success_shouldPublishCompletedEvent() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-EXEC-02");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-EXEC-02")).thenReturn(Optional.of(payment));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(successResult("REQ-002"));

        service.execute(jwt, "FP-VTP-EXEC-02", null);

        verify(eventPublisher, times(1)).publish(eq("payment.completed"), any());
        verify(eventPublisher, never()).publish(eq("payment.failed"), any());
    }

    // ─── Wallet reversal ──────────────────────────────────────────────────────

    @Test
    void execute_walletRail_providerFailure_shouldReverseDebit() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-EXEC-03");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-EXEC-03")).thenReturn(Optional.of(payment));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedResult("REQ-003"));

        service.execute(jwt, "FP-VTP-EXEC-03", null);

        verify(ledgerService, times(1)).debit(any(), any(), any(), any());
        verify(ledgerService, times(1)).reverse(any(), anyString(), anyString());
    }

    @Test
    void execute_walletRail_providerFailure_shouldPublishFailedEvent() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-EXEC-04");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-EXEC-04")).thenReturn(Optional.of(payment));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(failedResult("REQ-004"));

        service.execute(jwt, "FP-VTP-EXEC-04", null);

        verify(eventPublisher, times(1)).publish(eq("payment.failed"), any());
        verify(eventPublisher, never()).publish(eq("payment.completed"), any());
    }

    // ─── Idempotent execution ─────────────────────────────────────────────────

    @Test
    void execute_duplicateIdempotencyKey_shouldReturnEarlyWithoutDebitOrProviderCall() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-EXEC-05");
        payment.setLastExecuteIdempotencyKey("idem-key-reuse-xyz");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-EXEC-05")).thenReturn(Optional.of(payment));

        service.execute(jwt, "FP-VTP-EXEC-05", "idem-key-reuse-xyz");

        verifyNoInteractions(ledgerService);
        verifyNoInteractions(vtpassClient);
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser buildUser() throws Exception {
        AppUser user = new AppUser(KEYCLOAK_ID, TENANT_ID, "08011111111", null);
        Field idField = AppUser.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, USER_ID);
        return user;
    }

    private VtpassPayment buildAuthorizedWalletPayment(String ref) {
        VtpassPayment payment = new VtpassPayment(
            USER_ID, TENANT_ID, ref, "airtel-airtime", "08011111111",
            new BigDecimal("500.00"), VtpassPaymentMethod.WALLET, null, null
        );
        payment.markAuthorized("authorized", null, null);
        payment.setProcessorId("wallet");
        return payment;
    }

    private VtpassPurchaseResult successResult(String requestId) {
        return new VtpassPurchaseResult(true, false, "000", "TRANSACTION SUCCESSFUL", "delivered",
                requestId, "TXN-001", "{\"code\":\"000\",\"requestId\":\"" + requestId + "\"}");
    }

    private VtpassPurchaseResult failedResult(String requestId) {
        return new VtpassPurchaseResult(false, false, "016", "Transaction failed", "failed",
                requestId, null, "{\"code\":\"016\",\"requestId\":\"" + requestId + "\"}");
    }
}
