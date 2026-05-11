package ng.fixpay.core.kyc.provider;

import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;

public interface IdentityVerificationProvider {
    VerificationResult verifyCompanyAndDirectors(CompanyVerificationRequest request);
}
