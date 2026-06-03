package ng.fixpay.core.dispute.dto;

import ng.fixpay.core.dispute.domain.Dispute;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record DisputeResponse(
        UUID      id,
        String    transactionReference,
        String    category,
        String    description,
        String    status,
        String    resolution,
        Instant   slaDeadline,
        Instant   createdAt,
        TransactionSnapshot transaction
) {

    /**
     * Snapshot of the disputed transaction included on the dispute record.
     * Matches the PWA {@code Dispute.transaction} shape.
     */
    public record TransactionSnapshot(
            String     description,
            BigDecimal amountKobo,  // multiplied × 100 from NGN for PWA compatibility
            Instant    createdAt
    ) {}

    public static DisputeResponse from(Dispute d) {
        TransactionSnapshot snap = null;
        if (d.getTransactionAmount() != null) {
            // PWA expects amountKobo; internally we store NGN → multiply × 100
            BigDecimal amountKobo = d.getTransactionAmount().multiply(BigDecimal.valueOf(100));
            snap = new TransactionSnapshot(d.getTransactionDescription(), amountKobo, d.getTransactionDate());
        }
        return new DisputeResponse(
                d.getId(),
                d.getTransactionReference(),
                d.getCategory().name(),
                d.getDescription(),
                d.getStatus().name(),
                d.getResolution(),
                d.getSlaDeadline(),
                d.getCreatedAt(),
                snap
        );
    }
}
