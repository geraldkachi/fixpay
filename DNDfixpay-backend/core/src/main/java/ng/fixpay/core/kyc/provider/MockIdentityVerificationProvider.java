package ng.fixpay.core.kyc.provider;

import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@ConditionalOnProperty(name = "fixpay.verification.provider", havingValue = "mock", matchIfMissing = true)
public class MockIdentityVerificationProvider implements IdentityVerificationProvider {

    @Value("${fixpay.verification.report-base-url:https://reports.fixpay.local/kyc}")
    private String reportBaseUrl;

    @Override
    public VerificationResult verifyCompanyAndDirectors(CompanyVerificationRequest request) {
        boolean hasValidDirectors = request.directors() != null && !request.directors().isEmpty()
                && request.directors().stream().allMatch(d -> d.bvn().length() == 11 && d.nin().length() == 11);

        boolean validCac = request.cacRegistrationNumber() != null
                && request.cacRegistrationNumber().toUpperCase().matches("^[A-Z]{2,4}[0-9]{3,10}$");

        boolean passed = hasValidDirectors && validCac;
        String providerReference = "KYC-" + UUID.randomUUID();
        String reportUrl = reportBaseUrl + "/" + providerReference + ".pdf";

        return new VerificationResult(
                passed,
                providerReference,
                reportUrl,
                Instant.now(),
                passed ? "Verification passed" : "Verification failed"
        );
    }
}
