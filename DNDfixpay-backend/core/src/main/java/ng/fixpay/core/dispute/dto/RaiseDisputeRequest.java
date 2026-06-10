package ng.fixpay.core.dispute.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RaiseDisputeRequest(

        /** Payment reference of the disputed transaction (e.g. "FP-VTP-..."). */
        @NotBlank
        String transactionReference,

        /** One of: WRONG_AMOUNT, NOT_RECEIVED, DOUBLE_CHARGE, UNAUTHORIZED, OTHER */
        @NotBlank
        String category,

        @NotBlank
        @Size(min = 10, max = 1000, message = "Please provide at least 10 characters describing the issue")
        String description
) {}
