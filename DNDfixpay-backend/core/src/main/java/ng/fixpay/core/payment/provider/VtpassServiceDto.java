package ng.fixpay.core.payment.provider;

import java.math.BigDecimal;

/**
 * Lightweight projection of a VTpass service entry as returned by
 * {@code GET /api/services?identifier={category}}.
 *
 * <p>Presence in the VTpass list implies the service is currently active.
 * Absence means it is suspended or inactive.
 */
public record VtpassServiceDto(
        String serviceID,
        String name,
        BigDecimal minimumAmount,
        BigDecimal maximumAmount,
        String productType
) {}
