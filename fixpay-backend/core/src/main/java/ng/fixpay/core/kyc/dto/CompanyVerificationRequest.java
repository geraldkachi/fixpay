package ng.fixpay.core.kyc.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CompanyVerificationRequest(
        @NotBlank(message = "companyName is required")
        String companyName,

        @NotBlank(message = "cacRegistrationNumber is required")
        @Pattern(regexp = "^[A-Za-z]{2,4}[0-9]{3,10}$", message = "CAC registration number format is invalid")
        String cacRegistrationNumber,

        @Valid
        @Size(min = 1, max = 10, message = "at least one director identity is required")
        List<DirectorIdentityRequest> directors
) {}
