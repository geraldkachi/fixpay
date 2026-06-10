package ng.fixpay.core.payment.dto;

import java.math.BigDecimal;

public record InitializeVtpassPaymentResponse(
        String paymentReference,
        String paymentStatus,
        VtpassPaymentMethod paymentMethod,
        BigDecimal amount,
        String providerStatus,
        String authorizationMessage,
        String ussdCode,
        String checkoutUrl,
        String mandateReference
) {}
