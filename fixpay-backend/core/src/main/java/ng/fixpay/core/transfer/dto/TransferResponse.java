package ng.fixpay.core.transfer.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TransferResponse(
        UUID       transferId,
        String     reference,
        BigDecimal amount,
        BigDecimal fee,
        String     recipientName,
        String     recipientBank,
        String     narration,
        String     status,
        Instant    createdAt
) {}
