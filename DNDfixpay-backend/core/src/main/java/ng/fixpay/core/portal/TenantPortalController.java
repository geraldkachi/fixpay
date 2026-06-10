package ng.fixpay.core.portal;

import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Authenticated portal endpoint: tenant reads and updates their own profile.
 */
@RestController
@RequestMapping("/api/portal")
public class TenantPortalController {

    private final TenantRepository tenantRepository;

    public TenantPortalController(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    /** GET /api/portal/me — returns the authenticated tenant's profile */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<TenantProfileResponse>> getMe(@AuthenticationPrincipal Jwt jwt) {
        Tenant tenant = resolveTenant(jwt);
        return ResponseEntity.ok(ApiResponse.ok(TenantProfileResponse.from(tenant)));
    }

    /** PATCH /api/portal/me/branding — update branding fields */
    @PatchMapping("/me/branding")
    public ResponseEntity<ApiResponse<TenantProfileResponse>> updateBranding(
            @AuthenticationPrincipal Jwt jwt,
            @RequestBody BrandingRequest req) {
        Tenant tenant = resolveTenant(jwt);
        if (req.primaryColor()   != null) tenant.setPrimaryColor(req.primaryColor());
        if (req.secondaryColor() != null) tenant.setSecondaryColor(req.secondaryColor());
        if (req.accentColor()    != null) tenant.setAccentColor(req.accentColor());
        if (req.logoUrl()        != null) tenant.setLogoUrl(req.logoUrl());
        if (req.faviconUrl()     != null) tenant.setFaviconUrl(req.faviconUrl());
        if (req.supportEmail()   != null) tenant.setSupportEmail(req.supportEmail());
        if (req.supportPhone()   != null) tenant.setSupportPhone(req.supportPhone());
        tenantRepository.save(tenant);
        return ResponseEntity.ok(ApiResponse.ok(TenantProfileResponse.from(tenant)));
    }

    /** POST /api/portal/me/request-go-live */
    @PostMapping("/me/request-go-live")
    public ResponseEntity<ApiResponse<Void>> requestGoLive(@AuthenticationPrincipal Jwt jwt) {
        Tenant tenant = resolveTenant(jwt);
        if (tenant.getStatus() == Tenant.Status.ACTIVE) {
            throw FixPayException.conflict("Tenant is already live");
        }
        if (tenant.getGoLiveRequestedAt() != null) {
            throw FixPayException.conflict("Go-live request already submitted — awaiting platform review");
        }
        tenant.setGoLiveRequestedAt(java.time.Instant.now());
        tenantRepository.save(tenant);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Helpers ──────────────────────────────────────────────────────────────

    Tenant resolveTenant(Jwt jwt) {
        String tenantIdStr = jwt.getClaimAsString("tenant_id");
        if (tenantIdStr != null && !tenantIdStr.isBlank()) {
            return tenantRepository.findById(UUID.fromString(tenantIdStr))
                    .orElseThrow(() -> FixPayException.notFound("Tenant not found"));
        }

        // Fallback for environments where Keycloak does not map custom tenant_id attribute into the token.
        String email = jwt.getClaimAsString("email");
        if (email == null || email.isBlank()) {
            throw FixPayException.unauthorized("No tenant context in token");
        }

        return tenantRepository.findByContactEmailIgnoreCaseAndActiveTrue(email)
                .orElseThrow(() -> FixPayException.unauthorized("No tenant context in token"));
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record TenantProfileResponse(
            UUID   id,
            String slug,
            String name,
            String contactEmail,
            String status,
            String kybStatus,
            String plan,
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String logoUrl,
            String faviconUrl,
            String supportEmail,
            String supportPhone,
            long   sandboxWalletBalance,
            java.time.Instant goLiveRequestedAt,
            java.time.Instant wentLiveAt,
            java.time.Instant createdAt
    ) {
        static TenantProfileResponse from(Tenant t) {
            return new TenantProfileResponse(
                    t.getId(), t.getSlug(), t.getName(), t.getContactEmail(),
                    t.getStatus().name(), t.getKybStatus().name(), t.getPlan().name(),
                    t.getPrimaryColor(), t.getSecondaryColor(), t.getAccentColor(),
                    t.getLogoUrl(), t.getFaviconUrl(),
                    t.getSupportEmail(), t.getSupportPhone(),
                    t.getSandboxWalletBalance(),
                    t.getGoLiveRequestedAt(), t.getWentLiveAt(), t.getCreatedAt()
            );
        }
    }

    public record BrandingRequest(
            String primaryColor,
            String secondaryColor,
            String accentColor,
            String logoUrl,
            String faviconUrl,
            String supportEmail,
            String supportPhone
    ) {}
}

