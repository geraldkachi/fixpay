package ng.fixpay.core.admin.dto;

import ng.fixpay.core.tenant.domain.Tenant;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Admin-facing view of a Tenant — richer than the public TenantConfigResponse. */
public record TenantAdminResponse(
        UUID                id,
        String              slug,
        String              name,
        Tenant.Status       status,
        Tenant.Plan         plan,
        Tenant.KybStatus    kybStatus,
        String              contactEmail,
        String              supportEmail,
        String              supportPhone,
        Instant             suspendedAt,
        String              suspendedReason,
        Instant             goLiveRequestedAt,
        Instant             wentLiveAt,
        Map<String, Object> featureFlags,
        Map<String, Object> whitelabelConfig,
        Instant             createdAt,
        Instant             updatedAt
) {
    public static TenantAdminResponse from(Tenant t) {
        return new TenantAdminResponse(
                t.getId(),
                t.getSlug(),
                t.getName(),
                t.getStatus(),
                t.getPlan(),
                t.getKybStatus(),
                t.getContactEmail(),
                t.getSupportEmail(),
                t.getSupportPhone(),
                t.getSuspendedAt(),
                t.getSuspendedReason(),
                t.getGoLiveRequestedAt(),
                t.getWentLiveAt(),
                t.getFeatureFlags(),
                t.getWhitelabelConfig(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
