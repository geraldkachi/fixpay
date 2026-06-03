package ng.fixpay.core.transfer.dto;

import jakarta.validation.constraints.*;

public record NameEnquiryRequest(

        @NotBlank
        @Size(min = 10, max = 10, message = "Account number must be exactly 10 digits")
        String accountNumber,

        @NotBlank(message = "Bank code is required")
        String bankCode
) {}
