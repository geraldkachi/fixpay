package ng.fixpay.core.payment.rail.dto;

import ng.fixpay.core.payment.dto.VtpassPaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Request body for creating or updating a payment rail configuration row.
 * Used by {@link ng.fixpay.core.payment.rail.PaymentRailAdminController}.
 *
 * <p>Fields are validated in the controller; {@code configJson} is an opaque
 * JSON string — the admin wizard serialises it from the form fields defined by
 * {@link ng.fixpay.shared.payment.ConfigSchema}.
 */
public record PaymentRailConfigRequest(

        /** Tenant to configure. {@code null} = global default (applies to all tenants). */
        java.util.UUID tenantId,

        /** The payment method this processor handles. Required for create. */
        VtpassPaymentMethod paymentMethod,

        /**
         * Processor identifier. Must match a {@link ng.fixpay.shared.payment.PaymentRailAdapter#processorId()}
         * registered in the application context or a loaded PF4J plugin.
         */
        String processorId,

        /**
         * Lower number = higher priority (1 is tried first). Must be ≥ 1.
         */
        int priority,

        /**
         * Processor-specific configuration as a JSON string.
         * Keys are defined by {@link ng.fixpay.shared.payment.ConfigSchema#fields()}.
         * Sensitive values (type="password") are stored encrypted at the DB level (future work).
         */
        String configJson
) {}
