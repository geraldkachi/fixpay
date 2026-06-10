package ng.fixpay.core.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record NinVerificationRequest(
        @NotBlank
        @Pattern(regexp = "\\d{11}", message = "NIN must be exactly 11 digits")
        String nin
) {}
