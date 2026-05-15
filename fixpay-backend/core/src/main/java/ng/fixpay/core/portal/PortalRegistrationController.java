package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * Fully public portal registration endpoints.
 * No authentication required.
 */
@RestController
@RequestMapping("/api/portal")
public class PortalRegistrationController {

    private final TenantRepository      tenantRepository;
    private final TenantKeycloakClient  tenantKeycloakClient;

    public PortalRegistrationController(TenantRepository tenantRepository,
                                         TenantKeycloakClient tenantKeycloakClient) {
        this.tenantRepository     = tenantRepository;
        this.tenantKeycloakClient = tenantKeycloakClient;
    }

    // ─── Request / Response records ──────────────────────────────────────────

    public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 120)
        String businessName,

        @NotBlank @Pattern(regexp = "^[a-z0-9-]{3,60}$",
            message = "Slug must be 3–60 lowercase letters, numbers, or hyphens")
        String slug,

        @NotBlank @Email
        String email,

        @NotBlank @Size(min = 8, max = 128)
        String password
    ) {}

    public record RegisterResponse(UUID tenantId, String slug) {}

    public record CheckSlugRequest(@NotBlank String slug) {}
    public record CheckSlugResponse(boolean available) {}

    // ─── Endpoints ───────────────────────────────────────────────────────────

    /**
     * POST /api/portal/register
     * <p>
     * Creates a Tenant (status=SANDBOX) and a Keycloak TENANT_ADMIN user
     * in the {@code tenants} realm.
     */
    @PostMapping("/register")
    public ResponseEntity<ApiResponse<RegisterResponse>> register(
            @Valid @RequestBody RegisterRequest req) {

        // 1. Ensure slug uniqueness
        if (tenantRepository.findBySlugAndActiveTrue(req.slug()).isPresent()) {
            throw FixPayException.conflict("Slug '" + req.slug() + "' is already taken");
        }

        // 2. Create the Tenant row first (get an id)
        UUID tenantId = UUID.randomUUID();
        Tenant tenant = Tenant.selfRegister(tenantId, req.slug(), req.businessName(), req.email());
        tenantRepository.save(tenant);

        // 3. Create Keycloak user; on failure, rollback is handled by @Transactional on service (here we do
        //    it inline — if Keycloak fails the DB txn will have already committed, so we clean up manually).
        try {
            tenantKeycloakClient.createTenantAdmin(req.email(), req.password(), tenantId, req.businessName());
        } catch (Exception e) {
            // Rollback: delete the tenant row we just created
            tenantRepository.delete(tenant);
            throw e;
        }

        return ResponseEntity.ok(ApiResponse.ok(new RegisterResponse(tenantId, req.slug())));
    }

    /**
     * POST /api/portal/check-slug
     * <p>
     * Returns {@code {"available": true/false}} so the registration form
     * can validate slug uniqueness in real time.
     */
    @PostMapping("/check-slug")
    public ResponseEntity<ApiResponse<CheckSlugResponse>> checkSlug(
            @Valid @RequestBody CheckSlugRequest req) {
        boolean taken = tenantRepository.findBySlugAndActiveTrue(req.slug()).isPresent();
        return ResponseEntity.ok(ApiResponse.ok(new CheckSlugResponse(!taken)));
    }
}

