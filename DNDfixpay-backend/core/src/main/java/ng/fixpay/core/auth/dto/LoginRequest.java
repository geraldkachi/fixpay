package ng.fixpay.core.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(
        String phone,
        String email,
        @NotBlank @Size(min = 4) String password
) {}
