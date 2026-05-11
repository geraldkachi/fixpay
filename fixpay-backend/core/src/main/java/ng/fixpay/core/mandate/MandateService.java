package ng.fixpay.core.mandate;

import ng.fixpay.core.mandate.domain.NibssMandate;
import ng.fixpay.core.mandate.domain.NibssMandateRepository;
import ng.fixpay.core.mandate.dto.CreateMandateRequest;
import ng.fixpay.core.mandate.dto.MandateResponse;
import ng.fixpay.core.user.domain.AppUser;
import ng.fixpay.core.user.domain.UserRepository;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class MandateService {

    private final UserRepository userRepository;
    private final NibssMandateRepository mandateRepository;

    public MandateService(UserRepository userRepository, NibssMandateRepository mandateRepository) {
        this.userRepository = userRepository;
        this.mandateRepository = mandateRepository;
    }

    @Transactional
    public MandateResponse create(Jwt jwt, CreateMandateRequest request) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        String mandateReference = "NPSM-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        NibssMandate mandate = new NibssMandate(
                user.getId(),
                user.getTenantId(),
                mandateReference,
                request.bankCode(),
                request.accountNumber(),
                request.maxAmount(),
                request.startDate(),
                request.endDate()
        );

        // Local bootstrap state for now; real provider callback/sync will update this.
        mandate.updateStatus("active", "Mandate provisioned for debit authorization");
        mandateRepository.save(mandate);
        return toResponse(mandate);
    }

    @Transactional(readOnly = true)
    public List<MandateResponse> listMine(Jwt jwt) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        return mandateRepository.findByUserIdOrderByCreatedAtDesc(user.getId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public MandateResponse getMine(Jwt jwt, String mandateReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        NibssMandate mandate = mandateRepository.findByMandateReference(mandateReference)
                .orElseThrow(() -> FixPayException.notFound("Mandate"));

        if (!mandate.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot access another user's mandate");
        }

        return toResponse(mandate);
    }

    @Transactional
    public MandateResponse syncMine(Jwt jwt, String mandateReference) {
        UUID keycloakId = UUID.fromString(jwt.getSubject());
        AppUser user = userRepository.findByKeycloakId(keycloakId)
                .orElseThrow(() -> FixPayException.notFound("User"));

        NibssMandate mandate = mandateRepository.findByMandateReference(mandateReference)
                .orElseThrow(() -> FixPayException.notFound("Mandate"));

        if (!mandate.getUserId().equals(user.getId())) {
            throw FixPayException.forbidden("You cannot sync another user's mandate");
        }

        // Placeholder sync behavior until external NIBSS status API is wired.
        mandate.updateStatus("active", "Mandate status synchronized");
        return toResponse(mandate);
    }

    @Transactional(readOnly = true)
    public boolean isActiveMandate(UUID userId, String mandateReference) {
        return mandateRepository.findByMandateReference(mandateReference)
                .filter(m -> m.getUserId().equals(userId))
                .filter(m -> "active".equals(m.getStatus()))
                .isPresent();
    }

    private MandateResponse toResponse(NibssMandate mandate) {
        return new MandateResponse(
                mandate.getMandateReference(),
                mandate.getBankCode(),
                mandate.getAccountNumber(),
                mandate.getMaxAmount(),
                mandate.getStatus(),
                mandate.getStartDate(),
                mandate.getEndDate(),
                mandate.getProviderMessage(),
                mandate.getCreatedAt(),
                mandate.getUpdatedAt()
        );
    }
}
