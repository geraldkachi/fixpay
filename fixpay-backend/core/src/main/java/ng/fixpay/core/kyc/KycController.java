package ng.fixpay.core.kyc;

import jakarta.validation.Valid;
import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;
import ng.fixpay.core.kyc.dto.CompanyVerificationResponse;
import ng.fixpay.core.kyc.dto.KycStatusResponse;
import ng.fixpay.shared.dto.ApiResponse;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/kyc")
public class KycController {

    private final KycService kycService;

    public KycController(KycService kycService) {
        this.kycService = kycService;
    }

    @PostMapping("/verify-company")
    public ApiResponse<CompanyVerificationResponse> verifyCompany(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CompanyVerificationRequest request
    ) {
        return ApiResponse.ok("Verification request submitted", kycService.verifyCompany(jwt, request));
    }

    @GetMapping("/status")
    public ApiResponse<KycStatusResponse> status(@AuthenticationPrincipal Jwt jwt) {
        return ApiResponse.ok(kycService.getStatus(jwt));
    }
}
