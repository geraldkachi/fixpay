package ng.fixpay.core.mandate.provider;

public record MandateProviderResult(
        String status,
        String providerMessage,
        String providerReference,
        String providerCode,
        String rawResponse
) {}
