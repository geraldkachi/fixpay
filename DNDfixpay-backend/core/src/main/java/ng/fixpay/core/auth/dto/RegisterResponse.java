package ng.fixpay.core.auth.dto;

import java.util.UUID;

public record RegisterResponse(
        UUID userId,
        UUID walletId,
        String phone,
        String message
) {}
