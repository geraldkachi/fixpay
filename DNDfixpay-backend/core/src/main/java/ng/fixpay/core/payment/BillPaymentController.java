package ng.fixpay.core.payment;

import jakarta.validation.Valid;
import ng.fixpay.core.payment.dto.BillPaymentRequest;
import ng.fixpay.core.payment.dto.BillPaymentResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Simplified bill-payment API consumed by the PWA.
 *
 * <p>Each endpoint accepts a product-specific JSON body, debits the user's wallet,
 * calls VTpass, and returns a receipt-friendly response. All payments use the
 * WALLET payment method — the two-step initialize/execute flow is handled
 * internally within a single transaction.</p>
 *
 * <p>Responses are returned directly (not wrapped in ApiResponse) because the
 * PWA reads {@code res.data.requestId} etc. without an envelope.</p>
 */
@RestController
@RequestMapping("/api/payments")
public class BillPaymentController {

    private final VtpassPaymentService paymentService;

    public BillPaymentController(VtpassPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    // ─── Catalogue ────────────────────────────────────────────────────────────

    /**
     * Returns service variation codes. The response body is forwarded as-is from
     * VTpass after normalising snake_case keys to camelCase.
     */
    @GetMapping(value = "/variations/{serviceId}", produces = MediaType.APPLICATION_JSON_VALUE)
    public String variations(@PathVariable String serviceId) {
        return paymentService.getServiceVariations(serviceId);
    }

    @PostMapping("/verify")
    public Map<String, Object> verify(@RequestBody Map<String, String> body) {
        return paymentService.verifyBiller(
                body.get("serviceId"),
                body.get("billersCode"),
                body.get("type"));
    }

    // ─── Payment endpoints ────────────────────────────────────────────────────

    @PostMapping("/airtime")
    public BillPaymentResponse airtime(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }

    @PostMapping("/data")
    public BillPaymentResponse data(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }

    @PostMapping("/tv")
    public BillPaymentResponse tv(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }

    @PostMapping("/electricity")
    public BillPaymentResponse electricity(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }

    @PostMapping("/education")
    public BillPaymentResponse education(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }

    @PostMapping("/insurance")
    public BillPaymentResponse insurance(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BillPaymentRequest request) {
        return paymentService.payImmediately(jwt, request);
    }
}
