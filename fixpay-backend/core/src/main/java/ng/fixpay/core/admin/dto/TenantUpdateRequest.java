package ng.fixpay.core.admin.dto;

import java.util.Map;

/** Request body for updating mutable tenant settings. */
public record TenantUpdateRequest(
        String              name,
        String              supportEmail,
        String              supportPhone,
        String              plan,       // Tenant.Plan name
        String              kybStatus,  // Tenant.KybStatus name
        Map<String, Object> featureFlags,
        Map<String, Object> whitelabelConfig
) {}
