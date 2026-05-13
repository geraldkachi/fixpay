package ng.fixpay.core.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.BillPaymentRequest;
import ng.fixpay.core.payment.dto.BillPaymentResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.payment.provider.VtpassPurchaseResult;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link VtpassPaymentService#payImmediately}.
 *
 * <p>Validates the single-step bill payment facade:
 * <ul>
 *   <li>Wallet debit on successful purchase</li>
 *   <li>Wallet reversal when VTpass returns a failure</li>
 *   <li>Insufficient balance check</li>
 *   <li>Pending (002 / not yet delivered) treated as success (no reversal)</li>
 *   <li>Payment journal entries are written for each state transition</li>
 * </ul>
 */
@ExtendWith(MockitoExtension.class)
class BillPaymentServiceTest {

    @Mock UserRepository userRepository;
    @Mock VtpassPaymentRepository paymentRepository;
    @Mock PaymentJournalEntryRepository journalRepository;
    @Mock WalletRepository walletRepository;
    @Mock VtpassClient vtpassClient;
    @Mock MandateService mandateService;
    @Mock DomainEventPublisher eventPublisher;
    @Mock Jwt jwt;

    private VtpassPaymentService service;

    private static final UUID KEYCLOAK_ID = UUID.randomUUID();
    private static final UUID USER_ID     = UUID.randomUUID();
    private static final UUID TENANT_ID   = UUID.randomUUID();
    private static final String REQUEST_ID = "20260511-abc123def456";

    @BeforeEach
    void setUp() {
        service = new VtpassPaymentService(
                userRepository, paymentRepository, journalRepository, walletRepository,
                vtpassClient, new ObjectMapper(), mandateService, eventPublisher,
                "dev_vtpass_webhook_secret"
        );
        when(jwt.getSubject()).thenReturn(KEYCLOAK_ID.toString());
    }

    // ─── Wallet debit ────────────────────────────────────────────────────────

    @Test
    void payImmediately_successfulPurchase_shouldDebitWallet() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(successResult(REQUEST_ID));

        BillPaymentRequest request = airtimeRequest(new BigDecimal("500.00"));

        BillPaymentResponse response = service.payImmediately(jwt, request);

        assertNotNull(response.requestId());
        // Balance must have decreased by ₦500
        assertEquals(new BigDecimal("4500.00"), wallet.getBalance());
        assertEquals(new BigDecimal("4500.00"), wallet.getLedgerBalance());
    }

    @Test
    void payImmediately_successfulPurchase_shouldPersistPaymentRecord() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(successResult(REQUEST_ID));

        ArgumentCaptor<VtpassPayment> captor = ArgumentCaptor.forClass(VtpassPayment.class);
        when(paymentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00")));

        VtpassPayment saved = captor.getValue();
        assertEquals("airtel", saved.getServiceId());
        assertEquals("08011111111", saved.getBillerCustomerRef());
        assertEquals(VtpassPaymentMethod.WALLET, saved.getPaymentMethod());
        assertEquals("completed", saved.getPaymentStatus());
    }

    // ─── Wallet reversal ─────────────────────────────────────────────────────

    @Test
    void payImmediately_providerFailure_shouldReverseWalletDebit() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(failedResult(REQUEST_ID));

        assertThrows(FixPayException.class,
                () -> service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00"))));

        // Balance must be restored to original amount
        assertEquals(new BigDecimal("5000.00"), wallet.getBalance());
        assertEquals(new BigDecimal("5000.00"), wallet.getLedgerBalance());
    }

    @Test
    void payImmediately_providerFailure_shouldMarkPaymentFailed() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(failedResult(REQUEST_ID));

        ArgumentCaptor<VtpassPayment> captor = ArgumentCaptor.forClass(VtpassPayment.class);
        when(paymentRepository.save(captor.capture())).thenAnswer(inv -> inv.getArgument(0));

        assertThrows(FixPayException.class,
                () -> service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00"))));

        assertEquals("failed", captor.getValue().getPaymentStatus());
    }

    // ─── Insufficient balance ─────────────────────────────────────────────────

    @Test
    void payImmediately_insufficientBalance_shouldThrowAndNotCallProvider() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("200.00")); // less than ₦500

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        FixPayException ex = assertThrows(FixPayException.class,
                () -> service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00"))));

        assertTrue(ex.getMessage().contains("Insufficient"), "Expected 'Insufficient' in message, got: " + ex.getMessage());
        verifyNoInteractions(vtpassClient);
        // Balance must not change
        assertEquals(new BigDecimal("200.00"), wallet.getBalance());
    }

    // ─── Pending result (not yet delivered) ──────────────────────────────────

    @Test
    void payImmediately_pendingResult_shouldNotReverseWallet() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(pendingResult(REQUEST_ID));

        // Should NOT throw — pending is not a failure
        BillPaymentResponse response = service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00")));

        assertNotNull(response);
        // Wallet must remain debited (not reversed) — VTpass will deliver asynchronously
        assertEquals(new BigDecimal("4500.00"), wallet.getBalance());
    }

    // ─── Journal audit trail ─────────────────────────────────────────────────

    @Test
    void payImmediately_successfulPurchase_shouldWriteWalletDebitJournalEntry() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(successResult(REQUEST_ID));

        service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00")));

        // At minimum: PAYMENT_INITIALIZED, WALLET_DEBITED, PROVIDER_COMPLETED
        verify(journalRepository, atLeast(3)).save(any());
    }

    @Test
    void payImmediately_providerFailure_shouldWriteWalletReversalJournalEntry() throws Exception {
        AppUser user   = buildUser();
        Wallet  wallet = buildWallet(user, new BigDecimal("5000.00"));

        when(userRepository.findByKeycloakId(KEYCLOAK_ID)).thenReturn(Optional.of(user));
        when(walletRepository.findByUserIdAndCurrency(USER_ID, "NGN")).thenReturn(Optional.of(wallet));
        when(paymentRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(journalRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(vtpassClient.purchase(any(), any(), any(), any(), any()))
                .thenReturn(failedResult(REQUEST_ID));

        assertThrows(FixPayException.class,
                () -> service.payImmediately(jwt, airtimeRequest(new BigDecimal("500.00"))));

        // At minimum: PAYMENT_INITIALIZED, WALLET_DEBITED, WALLET_REVERSED, PROVIDER_FAILED
        verify(journalRepository, atLeast(4)).save(any());
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    private AppUser buildUser() throws Exception {
        AppUser user = new AppUser(KEYCLOAK_ID, TENANT_ID, "08011111111", null);
        // Set JPA-managed id via reflection (entity constructor doesn't expose it)
        Field idField = AppUser.class.getDeclaredField("id");
        idField.setAccessible(true);
        idField.set(user, USER_ID);
        return user;
    }

    private Wallet buildWallet(AppUser user, BigDecimal balance) {
        Wallet wallet = new Wallet(user.getId(), user.getTenantId());
        wallet.setBalance(balance);
        wallet.setLedgerBalance(balance);
        return wallet;
    }

    private BillPaymentRequest airtimeRequest(BigDecimal amount) {
        return new BillPaymentRequest("airtel", "08011111111", null, amount, "08011111111", null);
    }

    private VtpassPurchaseResult successResult(String requestId) {
        return new VtpassPurchaseResult(true, false, "000", "TRANSACTION SUCCESSFUL", "delivered",
                requestId, "TXN-001", "{\"code\":\"000\",\"requestId\":\"" + requestId + "\"}");
    }

    private VtpassPurchaseResult pendingResult(String requestId) {
        return new VtpassPurchaseResult(false, true, "000", "Transaction in progress", "pending",
                requestId, null, "{\"code\":\"000\",\"requestId\":\"" + requestId + "\"}");
    }

    private VtpassPurchaseResult failedResult(String requestId) {
        return new VtpassPurchaseResult(false, false, "016", "Transaction failed", "failed",
                requestId, null, "{\"code\":\"016\",\"requestId\":\"" + requestId + "\"}");
    }
}
