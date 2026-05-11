package ng.fixpay.core.kyc.dto;

import java.time.Instant;

public record KycStatusResponse(
        String kycStatus,
        String verificationStatus,
        String reportUrl,
        String providerReference,
        Instant lastVerifiedAt
) {}
