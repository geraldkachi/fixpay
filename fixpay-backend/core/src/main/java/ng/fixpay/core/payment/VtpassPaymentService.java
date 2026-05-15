package ng.fixpay.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.ledger.LedgerService;
import ng.fixpay.core.ledger.LedgerService.DebitResult;
import ng.fixpay.core.mandate.MandateService;
import ng.fixpay.core.payment.domain.PaymentJournalEntry;
import ng.fixpay.core.payment.domain.PaymentJournalEntryRepository;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.BillPaymentRequest;
import ng.fixpay.core.payment.dto.BillPaymentResponse;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.PaymentJournalEntryResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.core.payment.dto.VtpassWebhookRequest;
import ng.fixpay.core.payment.provider.VtpassClient;
import ng.fixpay.core.payment.provider.VtpassPurchaseResult;
import ng.fixpay.core.payment.provider.VtpassServiceRegistry;
import ng.fixpay.core.payment.rail.FeeCalculatorService;
import ng.fixpay.core.payment.rail.PaymentRailRegistry;
import ng.fixpay.core.payment.rail.PaymentRailRegistry.ResolvedAdapter;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import ng.fixpay.shared.payment.PaymentRailRequest;
import ng.fixpay.shared.payment.PaymentRailResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class VtpassPaymentService {

    private final UserRepository userRepository;
    private final VtpassPaymentRepository paymentRepository;
    private final PaymentJournalEntryRepository paymentJournalRepository;
    private final LedgerService ledgerService;
    private final VtpassClient vtpassClient;
    private final VtpassServiceRegistry serviceRegistry;
    private final PaymentRailRegistry railRegistry;
    private final FeeCalculatorService feeCalculatorService;
    private final ObjectMapper objectMapper;
    private final MandateService mandateService;
    private final DomainEventPublisher eventPublisher;
    private final String webhookSecret;

    public VtpassPaymentService(
            UserRepository userRepository,
            VtpassPaymentRepository paymentRepository,
            PaymentJournalEntryRepository paymentJournalRepository,
            LedgerService ledgerService,
            VtpassClient vtpassClient,
            VtpassServiceRegistry serviceRegistry,
            PaymentRailRegistry railRegistry,
            FeeCalculatorService feeCalculatorService,
            ObjectMapper objectMapper,
            MandateService mandateService,
            DomainEventPublisher eventPublisher,
            @Value("${fixpay.vtpass.webhook-secret:dev_vtpass_webhook_secret}") String webhookSecret
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
        this.paymentJournalRepository = paymentJournalRepository;
        this.ledgerService = ledgerService;
        this.vtpassClient = vtpassClient;
        this.serviceRegistry = serviceRegistry;
        this.railRegistry = railRegistry;
        this.feeCalculatorService = feeCalculatorService;
        this.objectMapper = objectMapper;
        this.mandateService = mandateService;
        this.eventPublisher = eventPublisher;
        this.webhookSecret = webhookSecret;
    }

    @Transactional
    public InitializeVtpassPaymentResponse initialize(Jwt jwt, InitializeVtpassPaymentRequest request, String idempotencyKey) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        if (!serviceRegistry.isAvailable(request.serviceId())) {
            throw FixPayException.serviceUnavailable(
                    "Service '" + request.serviceId() + "' is currently unavailable. Please try again later.");
        }

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            var existing = paymentRepository.findByInitIdempotencyKeyAndUserId(idempotencyKey, user.getId());
            if (existing.isPresent()) {
                return toInitializeResponse(existing.get(), "Duplicate initialize request detected. Existing payment returned.", null, null);
            }
        }

        // ── Resolve the payment rail adapter ──────────────────────────────────
        ResolvedAdapter resolved = railRegistry.resolve(user.getTenantId(), request.paymentMethod());

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

        // ── Initiate the funding rail ──────────────────────────────────────────
        PaymentRailRequest railRequest = new PaymentRailRequest(
                user.getId(), user.getTenantId(), paymentReference,
                request.amount(), "NGN", request.billerCustomerRef(),
                request.variationCode(), request.mandateReference(), request.callbackUrl(),
                request.customerPhone(), request.subscriptionType(),
                resolved.processorConfig()
        );
        PaymentRailResult railResult;
        try {
            railResult = resolved.adapter().initiate(railRequest);
            railRegistry.recordSuccess(resolved.adapter().processorId());
        } catch (Exception e) {
            railRegistry.recordFailure(resolved.adapter().processorId(), e);
            throw e;
        }

        if (railResult.isFailed()) {
            throw FixPayException.badRequest(railResult.failureReason() != null
                    ? railResult.failureReason() : "Payment rail rejected the request");
        }

        // Build the stored authorization payload (variationCode + customerPhone + subscriptionType + rail metadata)
        Map<String, String> payload = new LinkedHashMap<>();
        if (request.variationCode() != null && !request.variationCode().isBlank()) {
            payload.put("variationCode", request.variationCode());
        }
        if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
            payload.put("customerPhone", request.customerPhone());
        }
        if (request.subscriptionType() != null && !request.subscriptionType().isBlank()) {
            payload.put("subscriptionType", request.subscriptionType());
        }
        if (railResult.externalReference() != null) {
            payload.put("railExternalRef", railResult.externalReference());
        }
        payload.put("processorId", resolved.adapter().processorId());

        // Store which processor handled this payment so execute() uses the same one
        payment.setProcessorId(resolved.adapter().processorId());

        if (railResult.isPending()) {
            payment.markPendingAuthorization("pending", railResult.externalReference(), toJson(payload));
        } else {
            payment.markAuthorized("ready", railResult.externalReference(), toJson(payload));
        }

        paymentRepository.save(payment);
        journal(
            payment,
            "PAYMENT_INITIALIZED",
            payment.getAmount(),
            null,
            null,
            "Payment initialized via " + resolved.adapter().processorId() + " rail",
            toJson(Map.of(
                "paymentMethod", payment.getPaymentMethod().name(),
                "processorId",   resolved.adapter().processorId(),
                "providerStatus", payment.getProviderStatus(),
                "idempotencyKey", Objects.toString(idempotencyKey, "")
            ))
        );
        publishPaymentEvent("payment.initialized", payment);

        return toInitializeResponse(payment,
                railResult.authorizationMessage(),
                railResult.ussdCode(),
                railResult.checkoutUrl());
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

        // ── Confirm funding via the rail adapter ──────────────────────────────
        // Use resolveById so we always confirm with the SAME processor used in initiate().
        String storedProcessorId = payment.getProcessorId();
        var resolvedForConfirm = (storedProcessorId != null)
                ? railRegistry.resolveById(storedProcessorId)
                        .map(a -> new ResolvedAdapter(a, parseProcessorConfig(payment.getAuthorizationPayload()), null))
                        .orElse(null)
                : null;
        if (resolvedForConfirm == null) {
            // Fallback: original resolve (e.g. processorId not yet stored on old payments)
            resolvedForConfirm = railRegistry.resolve(user.getTenantId(), payment.getPaymentMethod());
        }
        final ResolvedAdapter resolved = resolvedForConfirm;
        PaymentRailResult fundConfirm;
        try {
            fundConfirm = resolved.adapter().confirmFunded(paymentReference);
            railRegistry.recordSuccess(resolved.adapter().processorId());
        } catch (Exception e) {
            railRegistry.recordFailure(resolved.adapter().processorId(), e);
            throw e;
        }
        if (fundConfirm.isFailed()) {
            payment.markFailed("provider_rejected", "RAIL_UNCONFIRMED", fundConfirm.failureReason(), null);
            paymentRepository.save(payment);
            journal(payment, "RAIL_UNCONFIRMED", payment.getAmount(), null, null,
                    "Rail did not confirm funding: " + fundConfirm.failureReason(), null);
            publishPaymentEvent("payment.failed", payment);
            throw FixPayException.badRequest(fundConfirm.failureReason() != null
                    ? fundConfirm.failureReason() : "Payment not yet confirmed by the payment provider");
        }

        journal(payment, "PAYMENT_EXECUTION_STARTED", payment.getAmount(), null, null,
                "Payment execution started via " + resolved.adapter().processorId(),
                toJson(Map.of("paymentMethod", payment.getPaymentMethod().name(),
                              "processorId", resolved.adapter().processorId())));
        publishPaymentEvent("payment.execution.started", payment);

        // ── Ledger debit (runs in REQUIRES_NEW — committed before VTpass call) ──
        // Only for WALLET rail; all other rails collect funds externally.
        DebitResult debitResult = null;
        if (payment.getPaymentMethod() == VtpassPaymentMethod.WALLET) {
            debitResult = ledgerService.debit(
                    user.getId(),
                    payment.getAmount(),
                    payment.getPaymentReference(),
                    "Wallet debit for VTpass payment: " + payment.getServiceId()
            );
            journal(payment, "WALLET_DEBITED", payment.getAmount(),
                    debitResult.balanceBefore(), debitResult.balanceAfter(),
                    "Wallet debited via LedgerService", null);
        }

        // ── VTpass call ──────────────────────────────────────────────────────
        String requestId = generateRequestId();
        VtpassPurchaseResult result = vtpassClient.purchase(
                requestId,
                payment.getServiceId(),
                payment.getAmount(),
                payment.getBillerCustomerRef(),
                extractVariationCode(payment.getAuthorizationPayload()),
                extractCustomerPhone(payment.getAuthorizationPayload()),
                extractSubscriptionType(payment.getAuthorizationPayload())
        );

        if (result.successful()) {
            // Calculate and store processor fee for revenue tracking
            if (resolved.railConfigId() != null) {
                long feeKobo = feeCalculatorService.calculateFee(resolved.railConfigId(), payment.getAmount());
                payment.setProcessorFeeKobo(feeKobo);
            }
            payment.markCompleted(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_COMPLETED", payment.getAmount(), null, null,
                    "Provider completed payment", result.rawResponse());
            publishPaymentEvent("payment.completed", payment);
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_PENDING", payment.getAmount(), null, null,
                    "Provider accepted payment and marked it pending", result.rawResponse());
            publishPaymentEvent("payment.pending", payment);
        } else {
            // ── Provider failed — reverse the debit in a new transaction ──────
            if (debitResult != null) {
                ledgerService.reverse(debitResult, payment.getPaymentReference(),
                        "Provider failure: " + result.providerMessage());
                journal(payment, "WALLET_REVERSED", payment.getAmount(),
                        debitResult.balanceAfter(), debitResult.balanceBefore(),
                        "Wallet reversal after provider failure", null);
            }
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_FAILED", payment.getAmount(), null, null,
                    "Provider failed payment", result.rawResponse());
            publishPaymentEvent("payment.failed", payment);
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
            publishPaymentEvent("payment.completed", payment);
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "REQUERY_PENDING", payment.getAmount(), null, null, "Requery indicates pending", result.rawResponse());
            publishPaymentEvent("payment.pending", payment);
        } else {
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "REQUERY_FAILED", payment.getAmount(), null, null, "Requery indicates failure", result.rawResponse());
            publishPaymentEvent("payment.failed", payment);
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
    public VtpassPaymentStatusResponse processWebhook(String webhookSignature, String rawPayload) {
        if (!verifyHmac(webhookSignature, rawPayload)) {
            throw FixPayException.forbidden("Invalid webhook signature");
        }

        VtpassWebhookRequest request = parseWebhookRequest(rawPayload);

        VtpassPayment payment = paymentRepository.findByPaymentReference(request.paymentReference())
                .orElseThrow(() -> FixPayException.notFound("Payment"));

        String status = request.providerStatus() == null ? "" : request.providerStatus().toLowerCase();
        String externalRef = request.providerRequestId() == null || request.providerRequestId().isBlank()
                ? payment.getExternalReference()
                : request.providerRequestId();

        if ("delivered".equals(status) || "completed".equals(status) || "success".equals(status)) {
            payment.markCompleted(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_COMPLETED", payment.getAmount(), null, null, "Webhook completed payment", toJson(request));
            publishPaymentEvent("payment.completed", payment);
        } else if ("pending".equals(status) || "processing".equals(status)) {
            payment.markProcessing(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_PENDING", payment.getAmount(), null, null, "Webhook marked payment pending", toJson(request));
            publishPaymentEvent("payment.pending", payment);
        } else {
            payment.markFailed(request.providerStatus(), request.providerCode(), request.providerMessage(), externalRef);
            journal(payment, "WEBHOOK_FAILED", payment.getAmount(), null, null, "Webhook marked payment failed", toJson(request));
            publishPaymentEvent("payment.failed", payment);
        }

        return toStatusResponse(payment);
    }

        @Transactional(readOnly = true)
        public List<PaymentJournalEntryResponse> getJournal(Jwt jwt, String paymentReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
            .orElseThrow(() -> FixPayException.notFound("User"));

        VtpassPayment payment = paymentRepository.findByPaymentReference(paymentReference)
            .orElseThrow(() -> FixPayException.notFound("Payment"));

        if (!payment.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot access another user's payment journal");
        }

        return paymentJournalRepository.findByPaymentReferenceOrderByCreatedAtAsc(paymentReference)
            .stream()
            .map(entry -> new PaymentJournalEntryResponse(
                entry.getId(),
                entry.getEventType(),
                entry.getAmount(),
                entry.getBalanceBefore(),
                entry.getBalanceAfter(),
                entry.getNote(),
                entry.getPayload(),
                entry.getCreatedAt()
            ))
            .toList();
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

    private void publishPaymentEvent(String topic, VtpassPayment payment) {
        eventPublisher.publish(topic, Map.of(
                "paymentReference", payment.getPaymentReference(),
                "paymentStatus", payment.getPaymentStatus(),
                "providerStatus", payment.getProviderStatus(),
                "paymentMethod", payment.getPaymentMethod().name(),
                "amount", payment.getAmount().toPlainString(),
                "tenantId", payment.getTenantId().toString(),
                "userId", payment.getUserId().toString()
        ));
    }

    private VtpassWebhookRequest parseWebhookRequest(String rawPayload) {
        try {
            VtpassWebhookRequest request = objectMapper.readValue(rawPayload, VtpassWebhookRequest.class);
            if (request.paymentReference() == null || request.paymentReference().isBlank()) {
                throw FixPayException.badRequest("paymentReference is required");
            }
            if (request.providerStatus() == null || request.providerStatus().isBlank()) {
                throw FixPayException.badRequest("providerStatus is required");
            }
            return request;
        } catch (FixPayException ex) {
            throw ex;
        } catch (Exception ex) {
            throw FixPayException.badRequest("Invalid webhook payload");
        }
    }

    private boolean verifyHmac(String signature, String rawPayload) {
        if (signature == null || signature.isBlank() || rawPayload == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(rawPayload.getBytes(StandardCharsets.UTF_8));
            String expected = toHex(digest);

            String normalized = signature;
            if (normalized.startsWith("sha256=")) {
                normalized = normalized.substring("sha256=".length());
            }
            return normalized.equalsIgnoreCase(expected);
        } catch (Exception ex) {
            return false;
        }
    }

    private String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    private RailDecision decideRailAuthorization(InitializeVtpassPaymentRequest request, String paymentReference) {
        // NOTE: This method is retained only for reference during the rail migration.
        // It is no longer called. The PaymentRailRegistry + PaymentRailAdapter replaces it.
        String externalReference = "VTP-INIT-" + paymentReference;
        Map<String, String> payload = new LinkedHashMap<>();
        if (request.variationCode() != null && !request.variationCode().isBlank()) {
            payload.put("variationCode", request.variationCode());
        }
        if (request.customerPhone() != null && !request.customerPhone().isBlank()) {
            payload.put("customerPhone", request.customerPhone());
        }
        if (request.subscriptionType() != null && !request.subscriptionType().isBlank()) {
            payload.put("subscriptionType", request.subscriptionType());
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
            case BANK_TRANSFER, OPAY -> {
                payload.put("action", "external_checkout");
                yield new RailDecision(true, externalReference, payload, "Redirecting to external payment provider.", null, null);
            }
        };
    }

    /** Parses a limited config map from the stored authorization payload JSON (for resolveById fallback). */
    private Map<String, String> parseProcessorConfig(String authorizationPayload) {
        if (authorizationPayload == null || authorizationPayload.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(authorizationPayload,
                    new com.fasterxml.jackson.core.type.TypeReference<>() {});
        } catch (Exception ignored) {
            return Collections.emptyMap();
        }
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw FixPayException.badRequest("Unable to serialize authorization payload");
        }
    }

    private String generateRequestId() {
        // VTpass requires: first 12 chars numeric = YYYYMMDDHHII in Africa/Lagos (GMT+1)
        String prefix = ZonedDateTime.now(ZoneId.of("Africa/Lagos"))
                .format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 10);
        return prefix + suffix;
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

    private String extractCustomerPhone(String authorizationPayload) {
        if (authorizationPayload == null || authorizationPayload.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(authorizationPayload);
            var value = node.get("customerPhone");
            return value == null || value.isNull() ? null : value.asText();
        } catch (Exception ignored) {
            return null;
        }
    }

    private String extractSubscriptionType(String authorizationPayload) {
        if (authorizationPayload == null || authorizationPayload.isBlank()) {
            return null;
        }
        try {
            var node = objectMapper.readTree(authorizationPayload);
            var value = node.get("subscriptionType");
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

    // ─── Bill Payment Facade (simplified single-step API) ──────────────────

    /**
     * Initializes, authorizes, and executes a wallet-funded VTpass payment in a
     * single transaction. Intended for the simplified bill payment API
     * ({@code POST /api/payments/airtime}, {@code /data}, {@code /electricity}, etc.)
     */
    @Transactional
    public BillPaymentResponse payImmediately(Jwt jwt, BillPaymentRequest request) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        if (!serviceRegistry.isAvailable(request.serviceId())) {
            throw FixPayException.serviceUnavailable(
                    "Service '" + request.serviceId() + "' is currently unavailable. Please try again later.");
        }

        String billerRef = request.billersCode() != null && !request.billersCode().isBlank()
                ? request.billersCode()
                : request.phone();

        VtpassPaymentMethod effectiveMethod = request.effectivePaymentMethod();

        // The simplified API only supports synchronous rails. Async methods (CARD, USSD, etc.)
        // require the two-step initialize/execute flow via /api/payments/vtpass/initialize.
        if (effectiveMethod != VtpassPaymentMethod.WALLET) {
            throw FixPayException.badRequest(
                    "Payment method '" + effectiveMethod + "' requires the two-step flow. " +
                    "Use POST /api/payments/vtpass/initialize instead.");
        }

        // Resolve rail adapter (validates that WALLET rail is configured for this tenant)
        ResolvedAdapter resolved = railRegistry.resolve(user.getTenantId(), effectiveMethod);

        String paymentReference = "FP-VTP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        VtpassPayment payment = new VtpassPayment(
                user.getId(),
                user.getTenantId(),
                paymentReference,
                request.serviceId(),
                billerRef,
                request.amount(),
                effectiveMethod,
                null,
                null
        );
        payment.markAuthorized("ready", null,
                toJson(Map.of("action", "debit_wallet_and_purchase",
                        "variationCode", request.variationCode() != null ? request.variationCode() : "",
                        "processorId", resolved.adapter().processorId())));
        payment.setProcessorId(resolved.adapter().processorId());
        paymentRepository.save(payment);
        journal(payment, "PAYMENT_INITIALIZED", payment.getAmount(), null, null,
                "Immediate bill payment initialized via " + resolved.adapter().processorId(), null);

        // ── Ledger debit (committed in its own transaction) ──────────────────
        DebitResult debitResult = ledgerService.debit(
                user.getId(),
                payment.getAmount(),
                paymentReference,
                "Wallet debit for bill payment: " + request.serviceId()
        );
        journal(payment, "WALLET_DEBITED", payment.getAmount(),
                debitResult.balanceBefore(), debitResult.balanceAfter(),
                "Wallet debited via LedgerService", null);

        // ── VTpass call ──────────────────────────────────────────────────────
        String requestId = generateRequestId();
        VtpassPurchaseResult result = vtpassClient.purchase(
                requestId,
                request.serviceId(),
                request.amount(),
                billerRef,
                request.variationCode(),
                request.phone(),          // customer notification phone
                request.subscriptionType() // TV: "change" or "renew"
        );

        if (result.successful()) {
            // Track processor fee for billing/settlement module
            if (resolved.railConfigId() != null) {
                long feeKobo = feeCalculatorService.calculateFee(resolved.railConfigId(), request.amount());
                payment.setProcessorFeeKobo(feeKobo);
            }
            payment.markCompleted(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_COMPLETED", payment.getAmount(), null, null,
                    "Provider completed bill payment", result.rawResponse());
            publishPaymentEvent("payment.completed", payment);
        } else if (result.pending()) {
            payment.markProcessing(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_PENDING", payment.getAmount(), null, null,
                    "Provider pending bill payment", result.rawResponse());
            publishPaymentEvent("payment.pending", payment);
        } else {
            // ── Provider failed — reverse the debit ───────────────────────────
            ledgerService.reverse(debitResult, paymentReference,
                    "Provider failure: " + result.providerMessage());
            journal(payment, "WALLET_REVERSED", payment.getAmount(),
                    debitResult.balanceAfter(), debitResult.balanceBefore(),
                    "Wallet reversal after provider failure", null);
            payment.markFailed(result.providerStatus(), result.providerCode(), result.providerMessage(), result.requestId());
            journal(payment, "PROVIDER_FAILED", payment.getAmount(), null, null,
                    "Provider failed bill payment", result.rawResponse());
            publishPaymentEvent("payment.failed", payment);
            throw FixPayException.badRequest(
                    result.providerMessage() != null ? result.providerMessage() : "Payment declined by provider");
        }

        return parseBillPaymentResponse(result.rawResponse(), result.requestId(), request.amount());
    }

    /** Fetches service variations from VTpass and normalises field names for the frontend. */
    public String getServiceVariations(String serviceId) {
        String raw = vtpassClient.getVariations(serviceId);
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode varArray = root.path("content").path("variations");
            List<Map<String, Object>> list = new ArrayList<>();
            for (JsonNode v : varArray) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("variationCode",   text(v, "variation_code"));
                item.put("name",            text(v, "name"));
                item.put("variationAmount", text(v, "variation_amount"));
                item.put("fixedPrice",      text(v, "fixedPrice"));
                list.add(item);
            }
            return objectMapper.writeValueAsString(Map.of("variations", list));
        } catch (Exception ex) {
            // Return raw if parsing fails
            return raw;
        }
    }

    /** Proxies a VTpass merchant-verify call and normalises the response. */
    public Map<String, Object> verifyBiller(String serviceId, String billersCode, String type) {
        String raw = vtpassClient.merchantVerify(serviceId, billersCode, type);
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode content = root.path("content");
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("customerName",   firstNonNull(content, "Customer_Name", "name"));
            result.put("status",         firstNonNull(content, "Status", "status"));
            result.put("currentBouquet", firstNonNull(content, "Current_Bouquet", "Bouquet_Code"));
            result.put("renewalAmount",  parseDoubleOrNull(firstNonNull(content, "Renewal_Amount", "renewal_amount")));
            result.put("meterType",      firstNonNull(content, "Meter_Type", "meter_type"));
            result.put("accountType",    firstNonNull(content, "Customer_Account_Type", "account_type"));
            result.put("address",        firstNonNull(content, "Address", "address"));
            result.put("meterNumber",    firstNonNull(content, "Meter_Number", "meter_number"));
            return result;
        } catch (Exception ex) {
            throw FixPayException.badRequest("Biller verification failed: " + ex.getMessage());
        }
    }

    private BillPaymentResponse parseBillPaymentResponse(String rawResponse, String requestId, BigDecimal amount) {
        String token = null;
        String units = null;
        String pin   = null;
        String purchasedCode = null;
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            token         = text(root, "token");
            units         = text(root, "units");
            pin           = text(root, "Pin");
            purchasedCode = text(root, "purchased_code");
            if (token == null) token = text(root.path("content").path("transactions"), "token");
        } catch (Exception ignored) { /* use null values */ }
        return new BillPaymentResponse(
                requestId,
                Instant.now().toString(),
                amount.toPlainString(),
                token,
                units,
                pin,
                purchasedCode
        );
    }

    private String firstNonNull(JsonNode node, String... keys) {
        for (String key : keys) {
            String v = text(node, key);
            if (v != null && !v.isBlank()) return v;
        }
        return null;
    }

    private Double parseDoubleOrNull(String val) {
        if (val == null) return null;
        try { return Double.parseDouble(val); } catch (NumberFormatException e) { return null; }
    }

    private String text(JsonNode node, String key) {
        JsonNode value = node.get(key);
        return value == null || value.isNull() ? null : value.asText();
    }
}
