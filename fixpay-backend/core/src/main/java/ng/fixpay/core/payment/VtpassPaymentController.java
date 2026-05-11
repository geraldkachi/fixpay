package ng.fixpay.core.payment;

import jakarta.validation.Valid;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentRequest;
import ng.fixpay.core.payment.dto.InitializeVtpassPaymentResponse;
import ng.fixpay.core.payment.dto.VtpassPaymentStatusResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

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
            @Valid @RequestBody InitializeVtpassPaymentRequest request
    ) {
        return ApiResponse.ok("VTpass payment initialized", paymentService.initialize(jwt, request));
    }

    @GetMapping("/{paymentReference}")
    public ApiResponse<VtpassPaymentStatusResponse> status(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable String paymentReference
    ) {
        return ApiResponse.ok(paymentService.getStatus(jwt, paymentReference));
    }
}
