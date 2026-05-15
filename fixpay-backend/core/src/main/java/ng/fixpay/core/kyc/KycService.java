package ng.fixpay.core.kyc;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ng.fixpay.core.events.DomainEventPublisher;
import ng.fixpay.core.kyc.domain.KycVerification;
import ng.fixpay.core.kyc.domain.KycVerificationRepository;
import ng.fixpay.core.kyc.dto.CompanyVerificationRequest;
import ng.fixpay.core.kyc.dto.CompanyVerificationResponse;
import ng.fixpay.core.kyc.dto.KycStatusResponse;
import ng.fixpay.core.kyc.provider.IdentityVerificationProvider;
import ng.fixpay.core.kyc.provider.VerificationResult;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class KycService {

    private final UserRepository userRepository;
    private final KycVerificationRepository verificationRepository;
    private final IdentityVerificationProvider verificationProvider;
    private final ObjectMapper objectMapper;
    private final DomainEventPublisher eventPublisher;

    public KycService(
            UserRepository userRepository,
            KycVerificationRepository verificationRepository,
            IdentityVerificationProvider verificationProvider,
            ObjectMapper objectMapper,
            DomainEventPublisher eventPublisher
    ) {
        this.userRepository = userRepository;
        this.verificationRepository = verificationRepository;
        this.verificationProvider = verificationProvider;
        this.objectMapper = objectMapper;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public CompanyVerificationResponse verifyCompany(Jwt jwt, CompanyVerificationRequest request) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        String directorsJson = toJson(request.directors());
        KycVerification verification = verificationRepository.save(new KycVerification(
                user.getId(),
                user.getTenantId(),
                request.companyName(),
                request.cacRegistrationNumber(),
                directorsJson
        ));

        VerificationResult providerResult = verificationProvider.verifyCompanyAndDirectors(request);
        if (providerResult.passed()) {
            verification.markVerified(providerResult.providerReference(), providerResult.reportUrl(), providerResult.verifiedAt());
            user.setKycStatus("verified");
            if (user.getTier() < 2) {
                user.setTier((short) 2);
            }
        } else {
            verification.markFailed(providerResult.providerReference(), providerResult.reportUrl(), providerResult.verifiedAt());
            user.setKycStatus("rejected");
        }

        eventPublisher.publish("kyc.status.updated", java.util.Map.of(
                "userId", user.getId().toString(),
                "tenantId", user.getTenantId().toString(),
                "kycStatus", user.getKycStatus(),
                "verificationStatus", verification.getVerificationStatus(),
                "providerReference", String.valueOf(verification.getProviderReference())
        ));

        return new CompanyVerificationResponse(
                verification.getId(),
                user.getKycStatus(),
                verification.getVerificationStatus(),
                verification.getProviderReference(),
                verification.getReportUrl(),
                verification.getVerifiedAt()
        );
    }

    @Transactional(readOnly = true)
    public KycStatusResponse getStatus(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        return verificationRepository.findTopByUserIdOrderByCreatedAtDesc(user.getId())
                .map(v -> new KycStatusResponse(
                        user.getKycStatus(),
                        v.getVerificationStatus(),
                        v.getReportUrl(),
                        v.getProviderReference(),
                        v.getVerifiedAt()
                ))
                .orElse(new KycStatusResponse(user.getKycStatus(), "not_started", null, null, null));
    }

    /**
     * Verify NIN in dev mode (pass-through without external verification).
     */
    @Transactional
    public java.util.Map<String, Object> verifyNin(String keycloakId) {
        markKycVerified(keycloakId, "NIN");
        return java.util.Map.of(
                "message", "NIN verified",
                "data", java.util.Map.of(
                        "firstName", "JOHN",
                        "lastName", "ADEYEMI",
                        "middleName", "CHUKWU",
                        "dateOfBirth", "1990-05-11",
                        "gender", "M"
                )
        );
    }

    /**
     * Verify BVN in dev mode (pass-through without external verification).
     */
    @Transactional
    public java.util.Map<String, Object> verifyBvn(String keycloakId) {
        markKycVerified(keycloakId, "BVN");
        return java.util.Map.of(
                "message", "BVN verified",
                "data", java.util.Map.of(
                        "firstName", "JOHN",
                        "lastName", "ADEYEMI",
                        "dateOfBirth", "1990-05-11",
                        "enrollmentBank", "Guaranty Trust Bank",
                        "kycLevel", "2"
                )
        );
    }

    /**
     * Verify selfie in dev mode (pass-through without external verification).
     */
    @Transactional
    public java.util.Map<String, Object> verifySelfie(String keycloakId) {
        markKycVerified(keycloakId, "Selfie");
        return java.util.Map.of(
                "message", "Liveness check passed",
                "score", 0.97
        );
    }

    private void markKycVerified(String keycloakId, String step) {
        UUID kycId = UUID.fromString(keycloakId);
        AppUser user = userRepository.findByKeycloakId(kycId)
                .orElseThrow(() -> FixPayException.notFound("User"));
        // In dev mode, all steps pass immediately
        user.setKycStatus("verified");
        if (user.getTier() < 2) {
            user.setTier((short) 2);
        }
        userRepository.save(user);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw FixPayException.badRequest("Unable to serialize directors payload");
        }
    }
}
