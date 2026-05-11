package ng.fixpay.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntry;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.core.payment.dto.VtpassWebhookRequest;
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
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VtpassPaymentService {

    private final UserRepository userRepository;
    private final VtpassPaymentRepository paymentRepository;
    private final PaymentJournalEntryRepository paymentJournalRepository;
    private final WalletRepository walletRepository;
    private final VtpassClient vtpassClient;
    private final ObjectMapper objectMapper;
    private final MandateService mandateService;
    private final String webhookSecret;

    public VtpassPaymentService(
            UserRepository userRepository,
            VtpassPaymentRepository paymentRepository,
            PaymentJournalEntryRepository paymentJournalRepository,
            WalletRepository walletRepository,
            VtpassClient vtpassClient,
            ObjectMapper objectMapper,
            MandateService mandateService,
            @Value("${fixpay.vtpass.webhook-secret:dev_vtpass_webhook_secret}") String webhookSecret
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.paymentJournalRepository = paymentJournalRepository;
        this.walletRepository = walletRepository;
        this.vtpassClient = vtpassClient;
        this.objectMapper = objectMapper;
        this.mandateService = mandateService;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public InitializeVtpassPaymentResponse initialize(Jwt jwt, InitializeVtpassPaymentRequest request, String idempotencyKey) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByInitIdempotencyKeyAndUserId(idempotencyKey, user.getId());
            if (existing.isPresent()) {
                return toInitializeResponse(existing.get(), "Duplicate initialize request detected. Existing payment returned.", null, null);
            }
        }

        if (request.paymentMethod() == VtpassPaymentMethod.NIBSS_MANDATE
                && (request.mandateReference() == null || request.mandateReference().isBlank())) {
            throw FixPayException.badRequest("mandateReference is required for NIBSS_MANDATE payment");
        }

        if (request.paymentMethod() == VtpassPaymentMethod.NIBSS_MANDATE
                && !mandateService.isActiveMandate(user.getId(), request.mandateReference())) {
            throw FixPayException.badRequest("Active mandate is required for NIBSS_MANDATE payment");
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
            request.mandateReference(),
            (idempotencyKey == null || idempotencyKey.isBlank()) ? null : idempotencyKey
        );

        RailDecision decision = decideRailAuthorization(request, paymentReference);
        if (decision.pendingAuthorization()) {
            payment.markPendingAuthorization("pending", decision.externalReference(), toJson(decision.payload()));
        } else {
            payment.markAuthorized("ready", decision.externalReference(), toJson(decision.payload()));
        }

        paymentRepository.save(payment);
        journal(
            payment,
            "PAYMENT_INITIALIZED",
            payment.getAmount(),
            null,
            null,
            "Payment initialized",
            toJson(Map.of(
                "paymentMethod", payment.getPaymentMethod().name(),
                "providerStatus", payment.getProviderStatus(),
                "idempotencyKey", Objects.toString(idempotencyKey, "")
            ))
        );

        return toInitializeResponse(payment, decision.authorizationMessage(), decision.ussdCode(), decision.checkoutUrl());
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
    public VtpassPaymentStatusResponse execute(Jwt jwt, String paymentReference, String executeIdempotencyKey) {
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

        if (executeIdempotencyKey != null && !executeIdempotencyKey.isBlank()) {
            if (executeIdempotencyKey.equals(payment.getLastExecuteIdempotencyKey())) {
                return toStatusResponse(payment);
            }
            payment.setLastExecuteIdempotencyKey(executeIdempotencyKey);
        }

        journal(
                payment,
                "PAYMENT_EXECUTION_STARTED",
                payment.getAmount(),
                null,
                null,
                "Payment execution started",
                toJson(Map.of("paymentMethod", payment.getPaymentMethod().name()))
        );

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
                journal(
                    payment,
                    "WALLET_DEBITED",
                    payment.getAmount(),
                    originalBalance,
                    wallet.getBalance(),
                    "Wallet debited for VTpass payment",
                    null
                );
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
            journal(
                    payment,
                    "PROVIDER_COMPLETED",
                    payment.getAmount(),
                    null,
                    null,
                    "Provider completed payment",
                    result.rawResponse()
            );
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(
                    payment,
                    "PROVIDER_PENDING",
                    payment.getAmount(),
                    null,
                    null,
                    "Provider accepted payment and marked it pending",
                    result.rawResponse()
            );
        } else {
            if (wallet != null && originalBalance != null && originalLedger != null) {
                wallet.setBalance(originalBalance);
                wallet.setLedgerBalance(originalLedger);
                journal(
                        payment,
                        "WALLET_REVERSED",
                        payment.getAmount(),
                        originalBalance.subtract(payment.getAmount()),
                        originalBalance,
                        "Wallet reversal after provider failure",
                        null
                );
            }
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(
                    payment,
                    "PROVIDER_FAILED",
                    payment.getAmount(),
                    null,
                    null,
                    "Provider failed payment",
                    result.rawResponse()
            );
        }

        return toStatusResponse(payment);
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
            journal(payment, "REQUERY_COMPLETED", payment.getAmount(), null, null, "Requery completed payment", result.rawResponse());
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "REQUERY_PENDING", payment.getAmount(), null, null, "Requery indicates pending", result.rawResponse());
        } else {
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "REQUERY_FAILED", payment.getAmount(), null, null, "Requery indicates failure", result.rawResponse());
        }

        return toStatusResponse(payment);
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
        journal(payment, "CALLBACK_AUTHORIZED", payment.getAmount(), null, null, "Callback moved payment to authorized", null);
        return execute(jwt, paymentReference, null);
    }

    @Transactional
    public VtpassPaymentStatusResponse processWebhook(String webhookSignature, VtpassWebhookRequest request) {
        if (webhookSignature == null || !webhookSignature.equals(webhookSecret)) {
            throw FixPayException.forbidden("Invalid webhook signature");
        }

        VtpassPayment payment = paymentRepository.findByPaymentReference(request.paymentReference())
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        String status = request.providerStatus() == null ? "" : request.providerStatus().toLowerCase();
        String externalRef = request.providerRequestId() == null || request.providerRequestId().isBlank()
                ? payment.getExternalReference()
                : request.providerRequestId();

        if ("delivered".equals(status) || "completed".equals(status) || "success".equals(status)) {
            payment.markCompleted(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_COMPLETED", payment.getAmount(), null, null, "Webhook completed payment", toJson(request));
        } else if ("pending".equals(status) || "processing".equals(status)) {
            payment.markProcessing(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_PENDING", payment.getAmount(), null, null, "Webhook marked payment pending", toJson(request));
        } else {
            payment.markFailed(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_FAILED", payment.getAmount(), null, null, "Webhook marked payment failed", toJson(request));
        }

        return toStatusResponse(payment);
    }

    private InitializeVtpassPaymentResponse toInitializeResponse(
            VtpassPayment payment,
            String authorizationMessage,
            String ussdCode,
            String checkoutUrl
    ) {
        return new InitializeVtpassPaymentResponse(
                payment.getPaymentReference(),
                payment.getPaymentStatus(),
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getProviderStatus(),
                authorizationMessage,
                ussdCode,
                checkoutUrl,
                payment.getMandateReference()
        );
    }

    private VtpassPaymentStatusResponse toStatusResponse(VtpassPayment payment) {
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

    private void journal(
            VtpassPayment payment,
            String eventType,
            BigDecimal amount,
            BigDecimal balanceBefore,
            BigDecimal balanceAfter,
            String note,
            String payload
    ) {
        paymentJournalRepository.save(new PaymentJournalEntry(
                payment.getId(),
                payment.getPaymentReference(),
                eventType,
                amount,
                balanceBefore,
                balanceAfter,
                note,
                payload
        ));
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
