package ng.fixpay.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyOtpRequest(
        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @NotBlank(message = "otp is required")
        @Pattern(regexp = "^\\d{6}$", message = "otp must be 6 digits")
        String otp
) {}
