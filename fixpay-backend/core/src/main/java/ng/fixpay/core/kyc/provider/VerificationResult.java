package ng.fixpay.core.kyc.provider;

import java.time.Instant;

public record VerificationResult(
        boolean passed,
        String providerReference,
        String reportUrl,
        Instant verifiedAt,
        String message
) {}
