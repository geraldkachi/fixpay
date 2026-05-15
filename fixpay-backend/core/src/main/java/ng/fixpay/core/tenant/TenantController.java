package ng.fixpay.core.tenant;

import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import ng.fixpay.core.tenant.dto.TenantConfigResponse;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tenant")
public class TenantController {

    private static final String DEFAULT_SLUG = "demo";

    private final TenantRepository tenantRepo;

    public TenantController(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    /**
     * Returns the branding/feature config for a tenant.
     * Callers may pass {@code X-Tenant-Slug} header to select a specific tenant;
     * if omitted, the "demo" tenant is returned (safe default for development).
     *
     * <p>This endpoint is public — it is called before the user is authenticated
     * so the PWA can apply the correct brand colours and know the tenantId to
     * include in the registration payload.
     */
    @GetMapping("/config")
    public ApiResponse<TenantConfigResponse> config(
            @RequestHeader(value = "X-Tenant-Slug", required = false) String slug) {

        String resolvedSlug = (slug != null && !slug.isBlank()) ? slug : DEFAULT_SLUG;

        Tenant tenant = tenantRepo.findBySlugAndActiveTrue(resolvedSlug)
                .orElseThrow(() -> FixPayException.notFound("Tenant not found: " + resolvedSlug));

        return ApiResponse.ok("OK", TenantConfigResponse.from(tenant));
    }
}
