package ng.fixpay.core.kyc;

import jakarta.validation.Valid;
import ng.fixpay.core.kyc.dto.BvnVerificationRequest;
import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;
import ng.fixpay.core.kyc.dto.CompanyVerificationResponse;
import ng.fixpay.core.kyc.dto.KycStatusResponse;
import ng.fixpay.core.kyc.dto.NinVerificationRequest;
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

    @PostMapping("/nin")
    public ApiResponse<?> verifyNin(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody NinVerificationRequest request
    ) {
        var result = kycService.verifyNin(jwt.getSubject());
        return ApiResponse.ok("NIN verified", result);
    }

    @PostMapping("/bvn")
    public ApiResponse<?> verifyBvn(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody BvnVerificationRequest request
    ) {
        var result = kycService.verifyBvn(jwt.getSubject());
        return ApiResponse.ok("BVN verified", result);
    }

    @PostMapping("/selfie")
    public ApiResponse<?> verifySelfie(@AuthenticationPrincipal Jwt jwt) {
        var result = kycService.verifySelfie(jwt.getSubject());
        return ApiResponse.ok("Liveness check passed", result);
    }
}
