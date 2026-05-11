package ng.fixpay.core.wallet.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record WalletBalanceResponse(
        UUID walletId,
        String currency,
        BigDecimal availableBalance,
        BigDecimal ledgerBalance,
        String status
) {}
