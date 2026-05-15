package ng.fixpay.core.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "tenantId is required")
        String tenantId,

        @NotBlank(message = "phone is required")
        @Pattern(regexp = "^\\+?[1-9]\\d{6,14}$", message = "phone must be a valid E.164 number")
        String phone,

        @NotBlank(message = "email is required")
        @Email(message = "email must be a valid address")
        String email,

        @NotBlank(message = "password is required")
        @Size(min = 8, message = "password must be at least 8 characters")
        String password
) {}
