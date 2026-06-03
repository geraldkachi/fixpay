package ng.fixpay.core.transfer.dto;

import jakarta.validation.constraints.*;

public record BankTransferRequest(

        @NotBlank
        @Size(min = 10, max = 10, message = "Account number must be exactly 10 digits")
        String accountNumber,

        @NotBlank(message = "Bank code is required")
        String bankCode,

        /**
         * Amount in kobo (smallest NGN unit). Min ₦100 (10 000 kobo), max ₦5 000 000 (500 000 000 kobo).
         */
        @NotNull
        @Min(value = 10_000, message = "Minimum transfer amount is ₦100")
        @Max(value = 500_000_000L, message = "Maximum transfer amount is ₦5,000,000")
        Long amountKobo,

        @Size(max = 100)
        String narration
) {}
