package ng.fixpay.core.payment;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.payment.domain.VtpassPayment;
import ng.fixpay.core.payment.domain.VtpassPaymentRepository;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class VtpassPaymentService {

    private final UserRepository userRepository;
    private final VtpassPaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public VtpassPaymentService(
            UserRepository userRepository,
            VtpassPaymentRepository paymentRepository,
            ObjectMapper objectMapper
    ) {
        this.userRepository = userRepository;
        this.paymentRepository = paymentRepository;
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
                payment.getPaymentMethod(),
                payment.getAmount(),
                payment.getExternalReference(),
                payment.getMandateReference(),
                payment.getCreatedAt(),
                payment.getUpdatedAt()
        );
    }

    private RailDecision decideRailAuthorization(InitializeVtpassPaymentRequest request, String paymentReference) {
        String externalReference = "VTP-INIT-" + paymentReference;
        Map<String, String> payload = new LinkedHashMap<>();

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

    private record RailDecision(
            boolean pendingAuthorization,
            String externalReference,
            Map<String, String> payload,
            String authorizationMessage,
            String ussdCode,
            String checkoutUrl
    ) {}
}
