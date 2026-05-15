package ng.fixpay.core.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record BvnVerificationRequest(
        @NotBlank
        @Pattern(regexp = "\\d{11}", message = "BVN must be exactly 11 digits")
        String bvn
) {}
