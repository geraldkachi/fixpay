package ng.fixpay.core.payment;

import jakarta.validation.Valid;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.PaymentJournalEntryResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments/vtpass")
public class VtpassPaymentController {

    private final VtpassPaymentService paymentService;

    public VtpassPaymentController(VtpassPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/initialize")
    public ApiResponse<InitializeVtpassPaymentResponse> initialize(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "x-idempotency-key", required = false) String idempotencyKey,
            @Valid @RequestBody InitializeVtpassPaymentRequest request
    ) {
        return ApiResponse.ok("VTpass payment initialized", paymentService.initialize(jwt, request, idempotencyKey));
    }

    @GetMapping("/{paymentReference}")
    public ApiResponse<VtpassPaymentStatusResponse> status(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok(paymentService.getStatus(jwt, paymentReference));
    }

    @PostMapping("/{paymentReference}/execute")
    public ApiResponse<VtpassPaymentStatusResponse> execute(
            @AuthenticationPrincipal Jwt jwt,
            @RequestHeader(name = "x-idempotency-key", required = false) String idempotencyKey,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok("VTpass payment execution attempted", paymentService.execute(jwt, paymentReference, idempotencyKey));
    }

    @PostMapping("/{paymentReference}/requery")
    public ApiResponse<VtpassPaymentStatusResponse> requery(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok("VTpass payment requery completed", paymentService.requery(jwt, paymentReference));
    }

    @PostMapping("/{paymentReference}/callback-authorize")
    public ApiResponse<VtpassPaymentStatusResponse> callbackAuthorize(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok("Authorization callback processed", paymentService.callbackAuthorize(jwt, paymentReference));
    }

    @PostMapping("/webhook")
    public ApiResponse<VtpassPaymentStatusResponse> webhook(
            @RequestHeader(name = "x-webhook-signature", required = false) String webhookSignature,
            @RequestBody String rawPayload
    ) {
        return ApiResponse.ok("Webhook processed", paymentService.processWebhook(webhookSignature, rawPayload));
    }

    @GetMapping("/{paymentReference}/journal")
    public ApiResponse<List<PaymentJournalEntryResponse>> journal(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok(paymentService.getJournal(jwt, paymentReference));
    }
}
