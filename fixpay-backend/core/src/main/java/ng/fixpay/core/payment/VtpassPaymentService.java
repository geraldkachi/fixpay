package ng.fixpay.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.payment.provider.VtpassPurchaseResult;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.core.wallet.domain.Wallet;
import ng.fixpay.core.wallet.domain.WalletRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VtpassPaymentService {

    private final UserRepository userRepository;
    private final VtpassPaymentRepository paymentRepository;
    private final WalletRepository walletRepository;
    private final VtpassClient vtpassClient;
    private final ObjectMapper objectMapper;

    public VtpassPaymentService(
            UserRepository userRepository,
            VtpassPaymentRepository paymentRepository,
            WalletRepository walletRepository,
            VtpassClient vtpassClient,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.walletRepository = walletRepository;
        this.vtpassClient = vtpassClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public InitializeVtpassPaymentResponse initialize(Jwt jwt, InitializeVtpassPaymentRequest request) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        if (request.paymentMethod() == VtpassPaymentMethod.NIBSS_MANDATE
                && (request.mandateReference() == null || request.mandateReference().isBlank())) {
            throw FixPayException.badRequest("mandateReference is required for NIBSS_MANDATE payment");
        }

        String paymentReference = "FP-VTP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        VtpassPayment payment = new VtpassPayment(
                user.getId(),
                user.getTenantId(),
                paymentReference,
                request.serviceId(),
                request.billerCustomerRef(),
                request.amount(),
                request.paymentMethod(),
                request.mandateReference()
        );

        RailDecision decision = decideRailAuthorization(request, paymentReference);
        if (decision.pendingAuthorization()) {
            payment.markPendingAuthorization("pending", decision.externalReference(), toJson(decision.payload()));
        } else {
            payment.markAuthorized("ready", decision.externalReference(), toJson(decision.payload()));
        }

        paymentRepository.save(payment);

        return new InitializeVtpassPaymentResponse(
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getProviderStatus(),
                decision.authorizationMessage(),
                decision.ussdCode(),
                decision.checkoutUrl(),
                payment.getMandateReference()
        );
    }

    @Transactional(readOnly = true)
    public VtpassPaymentStatusResponse getStatus(Jwt jwt, String paymentReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        VtpassPayment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        if (!payment.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot access another user's payment record");
        }

        return new VtpassPaymentStatusResponse(
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getProviderStatus(),
                payment.getProviderCode(),
                payment.getProviderMessage(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getExternalReference(),
                payment.getMandateReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @Transactional
    public VtpassPaymentStatusResponse execute(Jwt jwt, String paymentReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        VtpassPayment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        if (!payment.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot execute another user's payment");
        }

        if (!"authorized".equals(payment.getPaymentStatus())) {
            throw FixPayException.badRequest("Payment must be authorized before execution");
        }

        Wallet wallet = null;
        BigDecimal originalBalance = null;
        BigDecimal originalLedger = null;

        if (payment.getPaymentMethod() == VtpassPaymentMethod.WALLET) {
            wallet = walletRepository.findByUserIdAndCurrency(user.getId(), "NGN")
                    .orElseThrow(() -> FixPayException.notFound("Wallet"));

            originalBalance = wallet.getBalance();
            originalLedger = wallet.getLedgerBalance();
            if (wallet.getBalance().compareTo(payment.getAmount()) < 0) {
                throw FixPayException.badRequest("Insufficient wallet balance");
            }

            wallet.setBalance(wallet.getBalance().subtract(payment.getAmount()));
            wallet.setLedgerBalance(wallet.getLedgerBalance().subtract(payment.getAmount()));
        }

        String requestId = generateRequestId();
        VtpassPurchaseResult result = vtpassClient.purchase(
                requestId,
                payment.getServiceId(),
                payment.getAmount(),
                payment.getBillerCustomerRef(),
                extractVariationCode(payment.getAuthorizationPayload())
        );

        if (result.successful()) {
            payment.markCompleted(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        } else {
            if (wallet != null && originalBalance != null && originalLedger != null) {
                wallet.setBalance(originalBalance);
                wallet.setLedgerBalance(originalLedger);
            }
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        }

        return new VtpassPaymentStatusResponse(
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getProviderStatus(),
                payment.getProviderCode(),
                payment.getProviderMessage(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getExternalReference(),
                payment.getMandateReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @Transactional
    public VtpassPaymentStatusResponse requery(Jwt jwt, String paymentReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        VtpassPayment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        if (!payment.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot query another user's payment");
        }

        if (payment.getExternalReference() == null || payment.getExternalReference().isBlank()) {
            throw FixPayException.badRequest("No provider request id available for requery");
        }

        VtpassPurchaseResult result = vtpassClient.requery(payment.getExternalReference());
        if (result.successful()) {
            payment.markCompleted(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        } else {
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
        }

        return new VtpassPaymentStatusResponse(
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getProviderStatus(),
                payment.getProviderCode(),
                payment.getProviderMessage(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getExternalReference(),
                payment.getMandateReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    @Transactional
    public VtpassPaymentStatusResponse callbackAuthorize(Jwt jwt, String paymentReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        VtpassPayment payment = paymentRepository.findByPaymentReference(paymentReference)
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        if (!payment.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot authorize another user's payment");
        }

        if (!"pending_authorization".equals(payment.getPaymentStatus())) {
            throw FixPayException.badRequest("Payment is not waiting for authorization callback");
        }

        payment.markAuthorized("authorized", payment.getExternalReference(), payment.getAuthorizationPayload());
        return execute(jwt, paymentReference);
    }

    private RailDecision decideRailAuthorization(InitializeVtpassPaymentRequest request, String paymentReference) {
        String externalReference = "VTP-INIT-" + paymentReference;
        Map<String, String> payload = new LinkedHashMap<>();
        if (request.variationCode() != null && !request.variationCode().isBlank()) {
            payload.put("variationCode", request.variationCode());
        }

        return switch (request.paymentMethod()) {
            case WALLET -> {
                payload.put("action", "debit_wallet_and_purchase");
                payload.put("note", "Wallet rail selected. Debit and VTpass purchase can proceed immediately.");
                yield new RailDecision(false, externalReference, payload, "Wallet selected. Ready to debit and purchase.", null, null);
            }
            case USSD -> {
                String ussdCode = "*737*50*" + request.amount().toPlainString() + "#";
                payload.put("action", "collect_ussd_payment");
                payload.put("ussdCode", ussdCode);
                yield new RailDecision(true, externalReference, payload, "Dial the USSD code to complete payment authorization.", ussdCode, null);
            }
            case CARD -> {
                String checkoutUrl = "https://pay.fixpay.local/checkout/" + paymentReference;
                payload.put("action", "card_checkout");
                payload.put("checkoutUrl", checkoutUrl);
                yield new RailDecision(true, externalReference, payload, "Complete card authorization on the checkout page.", null, checkoutUrl);
            }
            case NIBSS_MANDATE -> {
                payload.put("action", "trigger_nibss_mandate_debit");
                payload.put("mandateReference", request.mandateReference());
                yield new RailDecision(true, externalReference, payload, "Mandate debit initialized. Awaiting NIBSS callback authorization.", null, null);
            }
        };
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw FixPayException.badRequest("Unable to serialize authorization payload");
        }
    }

    private String generateRequestId() {
        String datePrefix = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        return datePrefix + "-" + suffix;
    }

    private String extractVariationCode(String authorizationPayload) {
        if (authorizationPayload == null || authorizationPayload.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(authorizationPayload);
            var value = node.get("variationCode");
            return value == null || value.isNull() ? null : value.asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private record RailDecision(
            boolean pendingAuthorization,
            String externalReference,
            Map<String, String> payload,
            String authorizationMessage,
            String ussdCode,
            String checkoutUrl
    ) {}
}
