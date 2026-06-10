package ng.fixpay.core.mandate.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record MandateResponse(
        String mandateReference,
        String providerReference,
        String bankCode,
        String accountNumber,
        BigDecimal maxAmount,
        String status,
        LocalDate startDate,
        LocalDate endDate,
        String providerMessage,
        Instant createdAt,
        Instant updatedAt
) {}
