package ng.fixpay.core.transfer.dto;

import jakarta.validation.constraints.*;

public record WalletTransferRequest(

        @NotBlank(message = "Recipient phone is required")
        String recipientPhone,

        /**
         * Amount in kobo. Min ₦100 (10 000 kobo).
         */
        @NotNull
        @Min(value = 10_000, message = "Minimum transfer amount is ₦100")
        @Max(value = 500_000_000L, message = "Maximum transfer amount is ₦5,000,000")
        Long amountKobo,

        @Size(max = 100)
        String narration
) {}
