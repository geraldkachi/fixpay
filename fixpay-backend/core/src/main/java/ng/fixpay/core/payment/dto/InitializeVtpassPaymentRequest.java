package ng.fixpay.core.payment.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record InitializeVtpassPaymentRequest(
        @NotBlank(message = "serviceId is required")
        String serviceId,

        @NotBlank(message = "billerCustomerRef is required")
        String billerCustomerRef,

        @NotNull(message = "amount is required")
        @DecimalMin(value = "1.00", message = "amount must be at least 1.00")
        BigDecimal amount,

        @NotNull(message = "paymentMethod is required")
        VtpassPaymentMethod paymentMethod,

        String mandateReference,

        String callbackUrl
) {}
