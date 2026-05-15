package ng.fixpay.core.auth;

import jakarta.validation.Valid;
import ng.fixpay.core.auth.dto.CreatePinRequest;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/pin")
public class PinController {

    private final PinService pinService;

    public PinController(PinService pinService) {
        this.pinService = pinService;
    }

    @PostMapping("/create")
    public ApiResponse<Void> createPin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePinRequest req
    ) {
        pinService.createPin(jwt.getSubject(), req.pin());
        return ApiResponse.ok("PIN created", null);
    }

    @PostMapping("/verify")
    public ApiResponse<Void> verifyPin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreatePinRequest req
    ) {
        pinService.verifyPin(jwt.getSubject(), req.pin());
        return ApiResponse.ok("PIN verified", null);
    }
}
