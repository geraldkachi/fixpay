package ng.fixpay.core.payment.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PaymentJournalEntryResponse(
        UUID id,
        String eventType,
        BigDecimal amount,
        BigDecimal balanceBefore,
        BigDecimal balanceAfter,
        String note,
        String payload,
        Instant createdAt
) {}
