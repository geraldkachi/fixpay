package ng.fixpay.core.kyc.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record DirectorIdentityRequest(
        @NotBlank(message = "director fullName is required")
        String fullName,

        @NotBlank(message = "director BVN is required")
        @Pattern(regexp = "^\\d{11}$", message = "BVN must be 11 digits")
        String bvn,

        @NotBlank(message = "director NIN is required")
        @Pattern(regexp = "^\\d{11}$", message = "NIN must be 11 digits")
        String nin
) {}
