package ng.fixpay.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.ledger.LedgerService.DebitResult;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.BillPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.payment.provider.VtpassServiceRegistry;
import ng.fixpay.core.payment.rail.FeeCalculatorService;
import ng.fixpay.core.payment.rail.PaymentRailRegistry;
import ng.fixpay.core.payment.rail.PaymentRailRegistry.ResolvedAdapter;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import ng.fixpay.shared.payment.PaymentRailAdapter;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Failure-injection tests verifying graceful degradation when provider calls
 * throw exceptions (network timeouts, upstream outages).
 *
 * <p>Key behavioral contracts verified here:
 * <ul>
 *   <li>When {@code initiate()} or {@code confirmFunded()} throws,
 *       {@code PaymentRailRegistry.recordFailure()} is called for circuit-breaker
 *       tracking and the exception propagates to the caller.</li>
 *   <li>When {@code vtpassClient.purchase()} throws after the wallet has already
 *       been debited, no explicit {@code ledgerService.reverse()} is called.
 *       Recovery relies on {@code @Transactional} rollback in the production
 *       database context — not on a synchronous programmatic reversal.</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class VtpassProviderOutageTest {

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
        lenient().when(railRegistry.resolve(any(), any()))
                .thenReturn(new ResolvedAdapter(paymentRailAdapter, Map.of(), UUID.randomUUID()));
        lenient().when(ledgerService.debit(any(), any(), any(), any()))
                .thenReturn(new DebitResult(UUID.randomUUID(), USER_ID,
                        new BigDecimal("5000.00"), new BigDecimal("4500.00"),
                        new BigDecimal("500.00"), "corr-1"));
        lenient().when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(serviceRegistry.isAvailable(anyString())).thenReturn(true);
    }

    // ── initialize: rail adapter outage ──────────────────────────────────────

    /**
     * When the rail adapter throws during {@code initiate()}, the registry's
     * {@code recordFailure()} must be invoked for circuit-breaker tracking and
     * the payment must NOT be persisted.
     */
    @Test
    void initialize_railAdapterThrows_shouldRecordFailureAndNeverSavePayment() throws Exception {
        AppUser user = buildUser();
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRailAdapter.initiate(any(PaymentRailRequest.class)))
                .thenThrow(new RuntimeException("RAIL_DOWN: upstream timeout"));

        assertThrows(RuntimeException.class,
                () -> service.initialize(jwt, walletInitRequest(), null));

        verify(railRegistry, times(1)).recordFailure(eq("wallet"), any(RuntimeException.class));
        verify(paymentRepository, never()).save(any());
    }

    // ── execute: rail adapter outage ─────────────────────────────────────────

    /**
     * When {@code confirmFunded()} throws, {@code recordFailure()} must be called
     * for circuit-breaker tracking and the wallet debit must never be attempted
     * (debit occurs after confirmation in the execute flow).
     */
    @Test
    void execute_railAdapterConfirmFundedThrows_shouldRecordFailureAndNeverDebit() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-OUT-01");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-OUT-01")).thenReturn(Optional.of(payment));
        when(paymentRailAdapter.confirmFunded(any()))
                .thenThrow(new RuntimeException("RAIL_DOWN: connection refused"));

        assertThrows(RuntimeException.class,
                () -> service.execute(jwt, "FP-VTP-OUT-01", null));

        verify(railRegistry, times(1)).recordFailure(eq("wallet"), any(RuntimeException.class));
        verify(ledgerService, never()).debit(any(), any(), any(), any());
        verify(vtpassClient, never()).purchase(any(), any(), any(), any(), any(), any(), any());
    }

    // ── execute: VTpass outage after wallet debit ────────────────────────────

    /**
     * When {@code vtpassClient.purchase()} throws after the wallet debit has been
     * committed, no explicit {@code ledgerService.reverse()} call must be made.
     *
     * <p>Contrast with the <em>provider failure</em> case (VTpass returns a failure
     * result): that path calls {@code reverse()} explicitly before rethrowing.
     * When {@code purchase()} throws (network outage), the reverse block is never
     * reached — recovery relies on {@code @Transactional} rollback.
     */
    @Test
    void execute_vtpassPurchaseThrows_shouldPropagateWithoutExplicitReversal() throws Exception {
        AppUser user = buildUser();
        VtpassPayment payment = buildAuthorizedWalletPayment("FP-VTP-OUT-02");

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(paymentRepository.findByPaymentReference("FP-VTP-OUT-02")).thenReturn(Optional.of(payment));
        when(paymentRailAdapter.confirmFunded(any()))
                .thenReturn(PaymentRailResult.funded("RAIL-REF", null));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(FixPayException.badRequest("VTPass network error: connection timeout"));

        assertThrows(FixPayException.class,
                () -> service.execute(jwt, "FP-VTP-OUT-02", null));

        verify(ledgerService, times(1)).debit(any(), any(), any(), any());
        verify(ledgerService, never()).reverse(any(), anyString(), anyString());
    }

    // ── payImmediately: VTpass outage ────────────────────────────────────────

    /**
     * When {@code vtpassClient.purchase()} throws in the {@code payImmediately} path,
     * the exception propagates and no explicit {@code ledgerService.reverse()} is called.
     *
     * <p>When VTpass returns a failed result (provider decline), the service calls
     * {@code reverse()} explicitly. When {@code purchase()} throws (network outage),
     * the reverse block is never reached.
     */
    @Test
    void payImmediately_vtpassPurchaseThrows_shouldPropagateWithoutExplicitReversal() throws Exception {
        AppUser user = buildUser();
        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(vtpassClient.purchase(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(FixPayException.badRequest("VTPass network error: service unavailable"));

        assertThrows(FixPayException.class,
                () -> service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00"))));

        verify(ledgerService, times(1))
                .debit(eq(USER_ID), eq(new BigDecimal("500.00")), anyString(), anyString());
        verify(ledgerService, never()).reverse(any(), anyString(), anyString());
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

    private InitializeVtpassPaymentRequest walletInitRequest() {
        return new InitializeVtpassPaymentRequest(
                "airtel", "08011111111", null,
                new BigDecimal("500.00"), VtpassPaymentMethod.WALLET,
                null, null, "08011111111", null
        );
    }

    private BillPaymentRequest airtimeRequest(BigDecimal amount) {
        return new BillPaymentRequest(
                "airtel", "08011111111", null, amount,
                "08011111111", null, null
        );
    }
}
