package ng.fixpay.core.auth.dto;

import java.util.UUID;

public record VerifyOtpResponse(
        UUID   userId,
        String email,
        String message
) {}
