package ng.fixpay.core.health;

import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    @Value("${spring.application.name}")
    private String appName;

    @GetMapping("/health")
    public ApiResponse<Map<String, Object>> health() {
        return ApiResponse.ok(Map.of(
                "service",   appName,
                "status",    "UP",
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/version")
    public ApiResponse<Map<String, String>> version() {
        return ApiResponse.ok(Map.of(
                "version",     "0.1.0",
                "phase",       "0",
                "environment", System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "development")
        ));
    }
}
