package ng.fixpay.core.admin.dto;

import java.util.UUID;

public record CreateAdminUserRequest(
        String keycloakUserId,
        String username,
        String email,
        String role,       // AdminUser.AdminRole name
        UUID   tenantScope // null for all-tenants access
) {}
