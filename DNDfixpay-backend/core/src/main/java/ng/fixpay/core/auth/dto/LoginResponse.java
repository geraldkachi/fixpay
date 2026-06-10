package ng.fixpay.core.auth.dto;

import java.time.Instant;
import java.util.UUID;

public record LoginResponse(
        String accessToken,
        String refreshToken,
        UserDto user
) {
    public record UserDto(
            UUID   id,
            String phone,
            String email,
            String firstName,
            String lastName,
            int    tier,
            String kycStatus,
            Instant createdAt
    ) {}
}
