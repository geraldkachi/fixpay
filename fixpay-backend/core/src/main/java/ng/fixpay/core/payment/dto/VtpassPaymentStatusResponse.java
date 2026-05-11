package ng.fixpay.core.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record VtpassPaymentStatusResponse(
        String paymentReference,
        String paymentStatus,
        String providerStatus,
        VtpassPaymentMethod paymentMethod,
        BigDecimal amount,
        String externalReference,
        String mandateReference,
        Instant createdAt,
        Instant updatedAt
) {}
