package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import ng.fixpay.core.portal.domain.TenantKybSubmission;
import ng.fixpay.core.portal.domain.TenantKybSubmissionRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/kyb")
public class KybController {

    private final TenantKybSubmissionRepository kybRepo;
    private final TenantRepository              tenantRepository;
    private final TenantPortalController        portalHelper;

    public KybController(TenantKybSubmissionRepository kybRepo,
                          TenantRepository tenantRepository,
                          TenantPortalController portalHelper) {
        this.kybRepo          = kybRepo;
        this.tenantRepository = tenantRepository;
        this.portalHelper     = portalHelper;
    }

    /** GET /api/portal/kyb — get or create a draft submission */
    @GetMapping
    public ResponseEntity<ApiResponse<TenantKybSubmission>> get(@AuthenticationPrincipal Jwt jwt) {
        Tenant tenant = portalHelper.resolveTenant(jwt);
        TenantKybSubmission sub = kybRepo.findByTenantId(tenant.getId())
                .orElseGet(() -> kybRepo.save(new TenantKybSubmission(tenant.getId())));
        return ResponseEntity.ok(ApiResponse.ok(sub));
    }

    /** PATCH /api/portal/kyb — update draft fields */
    @PatchMapping
    public ResponseEntity<ApiResponse<TenantKybSubmission>> update(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody KybUpdateRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        TenantKybSubmission sub = kybRepo.findByTenantId(tenant.getId())
                .orElseGet(() -> new TenantKybSubmission(tenant.getId()));

        if (sub.getStatus() == TenantKybSubmission.Status.SUBMITTED
                || sub.getStatus() == TenantKybSubmission.Status.APPROVED) {
            throw FixPayException.conflict("KYB already submitted — cannot edit");
        }

        if (req.cacNumber()         != null) sub.setCacNumber(req.cacNumber());
        if (req.businessType()      != null) sub.setBusinessType(req.businessType());
        if (req.registeredAddress() != null) sub.setRegisteredAddress(req.registeredAddress());
        if (req.state()             != null) sub.setState(req.state());
        if (req.directors()         != null) sub.setDirectors(req.directors());
        if (req.documentUrls()      != null) sub.setDocumentUrls(req.documentUrls());

        return ResponseEntity.ok(ApiResponse.ok(kybRepo.save(sub)));
    }

    /** POST /api/portal/kyb/submit — submit for platform review */
    @PostMapping("/submit")
    public ResponseEntity<ApiResponse<TenantKybSubmission>> submit(@AuthenticationPrincipal Jwt jwt) {
        Tenant tenant = portalHelper.resolveTenant(jwt);
        TenantKybSubmission sub = kybRepo.findByTenantId(tenant.getId())
                .orElseThrow(() -> FixPayException.notFound("No KYB draft found — create one first"));

        if (sub.getStatus() != TenantKybSubmission.Status.DRAFT
                && sub.getStatus() != TenantKybSubmission.Status.REJECTED) {
            throw FixPayException.conflict("Cannot submit KYB in status: " + sub.getStatus());
        }
        sub.submit();
        tenant.setKybStatus(Tenant.KybStatus.IN_REVIEW);
        tenantRepository.save(tenant);
        return ResponseEntity.ok(ApiResponse.ok(kybRepo.save(sub)));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record KybUpdateRequest(
            String                    cacNumber,
            String                    businessType,
            String                    registeredAddress,
            String                    state,
            List<Map<String, String>> directors,
            Map<String, String>       documentUrls
    ) {}
}

