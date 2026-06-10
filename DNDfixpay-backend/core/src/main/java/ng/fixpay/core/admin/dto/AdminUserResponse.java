package ng.fixpay.core.admin.dto;

import ng.fixpay.core.admin.domain.AdminUser;

import java.time.Instant;
import java.util.UUID;

public record AdminUserResponse(
        UUID               id,
        String             keycloakUserId,
        String             username,
        String             email,
        AdminUser.AdminRole role,
        UUID               tenantScope,
        boolean            active,
        Instant            createdAt,
        Instant            lastLoginAt
) {
    public static AdminUserResponse from(AdminUser u) {
        return new AdminUserResponse(
                u.getId(),
                u.getKeycloakUserId(),
                u.getUsername(),
                u.getEmail(),
                u.getRole(),
                u.getTenantScope(),
                u.isActive(),
                u.getCreatedAt(),
                u.getLastLoginAt()
        );
    }
}
