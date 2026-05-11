package ng.fixpay.core.payment.dto;

import jakarta.validation.constraints.NotBlank;

public record VtpassWebhookRequest(
        @NotBlank(message = "paymentReference is required")
        String paymentReference,

        @NotBlank(message = "providerStatus is required")
        String providerStatus,

        String providerCode,
        String providerMessage,
        String providerRequestId
) {}
