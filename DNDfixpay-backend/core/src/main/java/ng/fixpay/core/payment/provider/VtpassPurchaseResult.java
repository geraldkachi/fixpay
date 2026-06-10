package ng.fixpay.core.payment.provider;

public record VtpassPurchaseResult(
        boolean successful,
        boolean pending,
        String providerCode,
        String providerMessage,
        String providerStatus,
        String requestId,
        String transactionId,
        String rawResponse
) {}
