package ng.fixpay.core.kyc.provider;

import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;
import ng.fixpay.core.kyc.dto.DirectorIdentityRequest;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Component
@ConditionalOnProperty(name = "fixpay.verification.provider", havingValue = "composite", matchIfMissing = false)
public class CompositeIdentityVerificationProvider implements IdentityVerificationProvider {

    private final BvnVerificationProvider bvnProvider;
    private final NinVerificationProvider ninProvider;
    private final CacVerificationProvider cacProvider;

    public CompositeIdentityVerificationProvider(
            BvnVerificationProvider bvnProvider,
            NinVerificationProvider ninProvider,
            CacVerificationProvider cacProvider) {
        this.bvnProvider = bvnProvider;
        this.ninProvider = ninProvider;
        this.cacProvider = cacProvider;
    }

    @Override
    public VerificationResult verifyCompanyAndDirectors(CompanyVerificationRequest request) {
        try {
            // Verify CAC registration
            CacVerificationProvider.RegistrationVerificationResult cacResult = cacProvider.verifyCac(
                    request.cacRegistrationNumber(),
                    request.companyName()
            );

            if (!cacResult.verified()) {
                return new VerificationResult(
                        false,
                        cacResult.reference(),
                        generateReportUrl(cacResult.reference()),
                        Instant.now(),
                        "CAC verification failed: " + cacResult.message()
                );
            }

            // Verify all directors' identities
            List<DirectorIdentityRequest> directors = request.directors();
            if (directors == null || directors.isEmpty()) {
                return new VerificationResult(
                        false,
                        cacResult.reference(),
                        generateReportUrl(cacResult.reference()),
                        Instant.now(),
                        "No directors provided for verification"
                );
            }

            List<String> failedDirectors = new ArrayList<>();
            for (DirectorIdentityRequest director : directors) {
                BvnVerificationProvider.IdentityVerificationResult bvnResult = bvnProvider.verifyBvn(
                        director.bvn(),
                        director.fullName()
                );

                if (!bvnResult.verified()) {
                    failedDirectors.add(director.fullName() + " (BVN failed)");
                    continue;
                }

                NinVerificationProvider.IdentityVerificationResult ninResult = ninProvider.verifyNin(
                        director.nin(),
                        director.fullName()
                );

                if (!ninResult.verified()) {
                    failedDirectors.add(director.fullName() + " (NIN failed)");
                }
            }

            if (!failedDirectors.isEmpty()) {
                return new VerificationResult(
                        false,
                        cacResult.reference(),
                        generateReportUrl(cacResult.reference()),
                        Instant.now(),
                        "Director verification failed for: " + String.join(", ", failedDirectors)
                );
            }

            // All verifications passed
            String providerReference = "COMPOSITE-" + UUID.randomUUID();
            return new VerificationResult(
                    true,
                    providerReference,
                    generateReportUrl(providerReference),
                    Instant.now(),
                    "All verifications passed successfully"
            );

        } catch (Exception ex) {
            throw new FixPayException("Verification process failed: " + ex.getMessage(), "VERIFICATION_ERROR", 500);
        }
    }

    private String generateReportUrl(String reference) {
        return "https://reports.fixpay.local/kyc/" + reference + ".pdf";
    }
}
