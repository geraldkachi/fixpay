package ng.fixpay.core.payment.rail.dto;

import ng.fixpay.core.payment.dto.VtpassPaymentMethod;
import ng.fixpay.core.payment.rail.domain.PaymentRailConfig;

import java.time.Instant;
import java.util.UUID;

/**
 * Read model returned by the admin API for a single payment rail configuration row.
 *
 * <p>The {@code configSchema} field is loaded from the registered adapter so the
 * admin wizard knows which form fields to render without hardcoding them in the UI.
 */
public record PaymentRailConfigResponse(
        UUID id,
        UUID tenantId,
        VtpassPaymentMethod paymentMethod,
        String processorId,
        int priority,
        boolean enabled,
        boolean maintenance,
        String configJson,
        ng.fixpay.shared.payment.ConfigSchema configSchema,
        Instant createdAt,
        Instant updatedAt
) {
    /** Maps a {@link PaymentRailConfig} entity + registry-supplied schema to this DTO. */
    public static PaymentRailConfigResponse from(PaymentRailConfig cfg,
                                                  ng.fixpay.shared.payment.ConfigSchema schema) {
        return new PaymentRailConfigResponse(
                cfg.getId(),
                cfg.getTenantId(),
                cfg.getPaymentMethod(),
                cfg.getProcessorId(),
                cfg.getPriority(),
                cfg.isEnabled(),
                cfg.isMaintenance(),
                cfg.getConfigJson(),
                schema,
                cfg.getCreatedAt(),
                cfg.getUpdatedAt()
        );
    }
}
