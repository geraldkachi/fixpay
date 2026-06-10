package ng.fixpay.core.admin;

import ng.fixpay.core.admin.dto.*;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Admin REST API for tenant management.
 *
 * <pre>
 * GET    /api/admin/tenants                    — paginated list with filters
 * GET    /api/admin/tenants/{id}               — tenant detail
 * PATCH  /api/admin/tenants/{id}               — update mutable fields
 * POST   /api/admin/tenants/{id}/suspend       — suspend with reason
 * POST   /api/admin/tenants/{id}/reactivate    — lift suspension
 * POST   /api/admin/tenants/{id}/offboard      — offboard permanently
 * PATCH  /api/admin/tenants/{id}/feature-flags — update feature flags JSONB
 * PATCH  /api/admin/tenants/{id}/whitelabel    — update whitelabel config JSONB
 * </pre>
 *
 * All endpoints require {@code PLATFORM_ADMIN} authority (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/tenants")
public class TenantAdminController {

    private final TenantRepository tenantRepo;

    public TenantAdminController(TenantRepository tenantRepo) {
        this.tenantRepo = tenantRepo;
    }

    @GetMapping
    public ApiResponse<Page<TenantAdminResponse>> list(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String plan,
            @RequestParam(required = false) String kybStatus,
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        Tenant.Status    statusEnum    = parse(status,    Tenant.Status.class);
        Tenant.Plan      planEnum      = parse(plan,      Tenant.Plan.class);
        Tenant.KybStatus kybStatusEnum = parse(kybStatus, Tenant.KybStatus.class);
        String normalizedSearch = search == null ? "" : search.trim().toLowerCase();
        boolean hasSearch = !normalizedSearch.isBlank();

        Page<TenantAdminResponse> result = tenantRepo
            .search(statusEnum, planEnum, kybStatusEnum, hasSearch, normalizedSearch,
                        PageRequest.of(page, Math.min(size, 100), Sort.by("createdAt").descending()))
                .map(TenantAdminResponse::from);

        return ApiResponse.ok(result);
    }

    @GetMapping("/{id}")
    public ApiResponse<TenantAdminResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(TenantAdminResponse.from(requireTenant(id)));
    }

    @PatchMapping("/{id}")
    public ApiResponse<TenantAdminResponse> update(
            @PathVariable UUID id,
            @RequestBody TenantUpdateRequest req) {

        Tenant t = requireTenant(id);

        if (req.name()           != null) t.setName(req.name());
        if (req.supportEmail()   != null) t.setSupportEmail(req.supportEmail());
        if (req.supportPhone()   != null) t.setSupportPhone(req.supportPhone());
        if (req.featureFlags()   != null) t.setFeatureFlags(req.featureFlags());
        if (req.whitelabelConfig() != null) t.setWhitelabelConfig(req.whitelabelConfig());
        if (req.plan()           != null) t.setPlan(Tenant.Plan.valueOf(req.plan()));
        if (req.kybStatus()      != null) t.setKybStatus(Tenant.KybStatus.valueOf(req.kybStatus()));

        return ApiResponse.ok("Updated", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    @PostMapping("/{id}/suspend")
    public ApiResponse<TenantAdminResponse> suspend(
            @PathVariable UUID id,
            @RequestBody SuspendTenantRequest req) {

        Tenant t = requireTenant(id);
        if (t.getStatus() == Tenant.Status.OFFBOARDED) {
            throw FixPayException.badRequest("Cannot suspend an offboarded tenant");
        }
        t.setStatus(Tenant.Status.SUSPENDED);
        t.setActive(false);
        t.setSuspendedAt(Instant.now());
        t.setSuspendedReason(req.reason());
        return ApiResponse.ok("Suspended", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    @PostMapping("/{id}/reactivate")
    public ApiResponse<TenantAdminResponse> reactivate(@PathVariable UUID id) {
        Tenant t = requireTenant(id);
        if (t.getStatus() == Tenant.Status.OFFBOARDED) {
            throw FixPayException.badRequest("Cannot reactivate an offboarded tenant");
        }
        t.setStatus(Tenant.Status.ACTIVE);
        t.setActive(true);
        t.setSuspendedAt(null);
        t.setSuspendedReason(null);
        return ApiResponse.ok("Reactivated", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    @PostMapping("/{id}/offboard")
    public ApiResponse<TenantAdminResponse> offboard(
            @PathVariable UUID id,
            @RequestBody SuspendTenantRequest req) {

        Tenant t = requireTenant(id);
        t.setStatus(Tenant.Status.OFFBOARDED);
        t.setActive(false);
        t.setSuspendedAt(Instant.now());
        t.setSuspendedReason(req.reason());
        return ApiResponse.ok("Offboarded", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    @PatchMapping("/{id}/feature-flags")
    public ApiResponse<TenantAdminResponse> updateFeatureFlags(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> flags) {

        Tenant t = requireTenant(id);
        t.setFeatureFlags(flags);
        return ApiResponse.ok("Feature flags updated", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    @PatchMapping("/{id}/whitelabel")
    public ApiResponse<TenantAdminResponse> updateWhitelabel(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, Object> config) {

        Tenant t = requireTenant(id);
        t.setWhitelabelConfig(config);
        return ApiResponse.ok("Whitelabel config updated", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    /**
     * POST /api/admin/tenants/{id}/approve-live
     * <p>
     * Approves the tenant's go-live request: status → ACTIVE, wentLiveAt = now.
     * This unlocks LIVE API keys, live webhooks, and live IP whitelist rules.
     */
    @PostMapping("/{id}/approve-live")
    public ApiResponse<TenantAdminResponse> approveLive(@PathVariable UUID id) {
        Tenant t = requireTenant(id);
        if (t.getStatus() == Tenant.Status.ACTIVE) {
            throw FixPayException.badRequest("Tenant is already live");
        }
        t.setStatus(Tenant.Status.ACTIVE);
        t.setActive(true);
        t.setWentLiveAt(Instant.now());
        return ApiResponse.ok("Go-live approved", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    /**
     * POST /api/admin/tenants/{id}/reject-live
     * <p>
     * Rejects the go-live request: status remains SANDBOX, goLiveRequestedAt is cleared
     * so the tenant may re-apply after addressing issues.
     */
    @PostMapping("/{id}/reject-live")
    public ApiResponse<TenantAdminResponse> rejectLive(
            @PathVariable UUID id,
            @RequestBody RejectLiveRequest req) {

        Tenant t = requireTenant(id);
        if (t.getStatus() == Tenant.Status.ACTIVE) {
            throw FixPayException.badRequest("Tenant is already live");
        }
        // Clear the requested flag so tenant can re-submit after fixing issues
        t.setGoLiveRequestedAt(null);
        // Note: you may publish an event/email here in a real impl
        return ApiResponse.ok("Go-live rejected", TenantAdminResponse.from(tenantRepo.save(t)));
    }

    public record RejectLiveRequest(String reason) {}

    // ─── Helpers ────────────────────────────────────────────────────────────

    private Tenant requireTenant(UUID id) {
        return tenantRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("Tenant " + id));
    }

    private static <E extends Enum<E>> E parse(String value, Class<E> type) {
        if (value == null || value.isBlank()) return null;
        try {
            return Enum.valueOf(type, value.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw FixPayException.badRequest("Invalid value '" + value + "' for " + type.getSimpleName());
        }
    }
}
