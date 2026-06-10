package ng.fixpay.core.kyc.dto;

import java.time.Instant;
import java.util.UUID;

public record CompanyVerificationResponse(
        UUID verificationId,
        String kycStatus,
        String verificationStatus,
        String providerReference,
        String reportUrl,
        Instant verifiedAt
) {}
