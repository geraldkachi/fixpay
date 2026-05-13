package ng.fixpay.core.payment.dto;

import jakarta.validation.constraints.NotBlank;

import java.math.BigDecimal;

public record BillPaymentRequest(
        @NotBlank(message = "serviceId is required")
        String serviceId,

        /** Meter number, smartcard number, phone number, or exam profile ID. */
        String billersCode,

        /** Variation / bundle code (null for fixed-amount products like airtime). */
        String variationCode,

        /** Required for amount-based products (airtime, electricity). Null if derived from variation. */
        BigDecimal amount,

        /** Phone number for airtime top-up (also used as billersCode when not provided separately). */
        String phone,

        /** TV only — "renew" or "change". */
        String subscriptionType
) {}
