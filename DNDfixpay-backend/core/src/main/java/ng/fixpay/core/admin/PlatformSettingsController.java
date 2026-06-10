package ng.fixpay.core.admin;

import ng.fixpay.core.payment.PaymentTimeoutProperties;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Admin REST API for platform-wide runtime settings.
 *
 * <pre>
 * GET  /api/admin/settings                     — list current settings
 * PUT  /api/admin/settings/payment-timeout      — update payment timeout threshold
 * </pre>
 *
 * All endpoints require {@code PLATFORM_ADMIN} authority (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/settings")
public class PlatformSettingsController {

    private final PaymentTimeoutProperties timeoutProperties;

    public PlatformSettingsController(PaymentTimeoutProperties timeoutProperties) {
        this.timeoutProperties = timeoutProperties;
    }

    /**
     * Returns the current live values of all configurable platform settings.
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> getSettings() {
        return ApiResponse.ok(Map.of(
                "paymentTimeoutSeconds", timeoutProperties.getTimeoutSeconds()
        ));
    }

    /**
     * Updates the payment timeout threshold at runtime without requiring a restart.
     * The change takes effect on the very next scheduler tick.
     *
     * <p>Accepted range: 1–3600 seconds.
     */
    @PutMapping("/payment-timeout")
    public ApiResponse<Map<String, Object>> updatePaymentTimeout(
            @RequestBody UpdatePaymentTimeoutRequest request) {
        if (request.timeoutSeconds() < 1 || request.timeoutSeconds() > 3600) {
            throw FixPayException.badRequest("timeoutSeconds must be between 1 and 3600");
        }
        timeoutProperties.setTimeoutSeconds(request.timeoutSeconds());
        return ApiResponse.ok(Map.of(
                "paymentTimeoutSeconds", timeoutProperties.getTimeoutSeconds()
        ));
    }

    public record UpdatePaymentTimeoutRequest(int timeoutSeconds) {}
}
