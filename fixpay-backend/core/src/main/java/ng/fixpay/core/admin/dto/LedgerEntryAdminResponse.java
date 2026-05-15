package ng.fixpay.core.admin.dto;

import ng.fixpay.core.ledger.domain.LedgerEntry;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record LedgerEntryAdminResponse(
        UUID              id,
        UUID              walletId,
        UUID              userId,
        UUID              tenantId,
        String            correlationId,
        LedgerEntry.EntryType entryType,
        BigDecimal        amount,
        BigDecimal        runningBalance,
        String            currency,
        String            reference,
        String            description,
        Instant           createdAt
) {
    public static LedgerEntryAdminResponse from(LedgerEntry e) {
        return new LedgerEntryAdminResponse(
                e.getId(),
                e.getWalletId(),
                e.getUserId(),
                e.getTenantId(),
                e.getCorrelationId(),
                e.getEntryType(),
                e.getAmount(),
                e.getRunningBalance(),
                e.getCurrency(),
                e.getReference(),
                e.getDescription(),
                e.getCreatedAt()
        );
    }
}
