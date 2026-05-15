package ng.fixpay.core.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import ng.fixpay.core.auth.dto.LoginRequest;
import ng.fixpay.core.auth.dto.LoginResponse;
import ng.fixpay.core.auth.dto.RegisterRequest;
import ng.fixpay.core.auth.dto.RegisterResponse;
import ng.fixpay.core.auth.dto.VerifyOtpRequest;
import ng.fixpay.core.auth.dto.VerifyOtpResponse;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private static final String REFRESH_COOKIE = "refresh_token";
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("User registered successfully", authService.register(req));
    }

    @PostMapping("/verify-otp")
    public ApiResponse<VerifyOtpResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest req) {
        return ApiResponse.ok("OTP verified", authService.verifyOtp(req));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(
            @RequestBody LoginRequest req,
            HttpServletResponse response
    ) {
        LoginResponse result = authService.login(req);
        if (result.refreshToken() != null && !result.refreshToken().isBlank()) {
            setRefreshCookie(response, result.refreshToken());
        }
        // Return accessToken + user but NOT the refreshToken in the body
        return ApiResponse.ok("Login successful", Map.of(
                "accessToken", result.accessToken(),
                "user", result.user()
        ));
    }

    @PostMapping("/refresh")
    public ApiResponse<Map<String, String>> refresh(
            HttpServletRequest request,
            HttpServletResponse response
    ) {
        String refreshToken = extractRefreshCookie(request);
        if (refreshToken == null || refreshToken.isBlank()) {
            throw FixPayException.unauthorized("No refresh token");
        }
        KeycloakAdminClient.TokenPair tokens = authService.refreshToken(refreshToken);
        setRefreshCookie(response, tokens.refreshToken());
        return ApiResponse.ok("Token refreshed", Map.of("accessToken", tokens.accessToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpServletResponse response) {
        clearRefreshCookie(response);
        return ApiResponse.ok("Logged out", null);
    }

    // ─── Cookie helpers ──────────────────────────────────────────────────────

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // set true in production (HTTPS)
        cookie.setPath("/api/auth");
        cookie.setMaxAge(60 * 60 * 24 * 30); // 30 days
        response.addCookie(cookie);
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie(REFRESH_COOKIE, "");
        cookie.setHttpOnly(true);
        cookie.setPath("/api/auth");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private String extractRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> REFRESH_COOKIE.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}

