package ng.fixpay.core.auth;

import jakarta.validation.Valid;
import ng.fixpay.core.auth.dto.RegisterRequest;
import ng.fixpay.core.auth.dto.RegisterResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RegisterResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ApiResponse.ok("User registered successfully", authService.register(req));
    }
}
