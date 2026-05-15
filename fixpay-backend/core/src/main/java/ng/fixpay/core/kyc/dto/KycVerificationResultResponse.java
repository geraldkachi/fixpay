package ng.fixpay.core.kyc.dto;

import java.util.Map;

public record KycVerificationResultResponse(
        String message,
        Map<String, String> data
) {}
