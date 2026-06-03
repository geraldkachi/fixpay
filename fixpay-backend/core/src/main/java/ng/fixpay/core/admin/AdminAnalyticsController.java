package ng.fixpay.core.admin;

import ng.fixpay.core.admin.dto.AnalyticsResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Platform-wide analytics summary for the admin dashboard.
 * Requires {@code PLATFORM_ADMIN} authority (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/analytics")
public class AdminAnalyticsController {

    private final AdminAnalyticsService analyticsService;

    public AdminAnalyticsController(AdminAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    @GetMapping
    public ApiResponse<AnalyticsResponse> analytics() {
        return ApiResponse.ok(analyticsService.getAnalytics());
    }
}
