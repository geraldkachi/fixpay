package ng.fixpay.core.admin;

import ng.fixpay.core.admin.domain.AdminUser;
import ng.fixpay.core.admin.domain.AdminUserRepository;
import ng.fixpay.core.admin.dto.AdminUserResponse;
import ng.fixpay.core.admin.dto.CreateAdminUserRequest;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Admin REST API for managing FixPay admin users.
 *
 * <pre>
 * GET    /api/admin/users        — list all admin users
 * GET    /api/admin/users/{id}   — get admin user
 * POST   /api/admin/users        — register a Keycloak user as admin
 * PATCH  /api/admin/users/{id}/role   — change role
 * PATCH  /api/admin/users/{id}/deactivate — soft-deactivate
 * PATCH  /api/admin/users/{id}/activate   — reactivate
 * </pre>
 *
 * Requires {@code PLATFORM_ADMIN} authority (enforced by SecurityConfig).
 */
@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserRepository adminUserRepo;

    public AdminUserController(AdminUserRepository adminUserRepo) {
        this.adminUserRepo = adminUserRepo;
    }

    @GetMapping
    public ApiResponse<List<AdminUserResponse>> list() {
        return ApiResponse.ok(adminUserRepo.findAll().stream()
                .map(AdminUserResponse::from)
                .toList());
    }

    @GetMapping("/{id}")
    public ApiResponse<AdminUserResponse> get(@PathVariable UUID id) {
        return ApiResponse.ok(AdminUserResponse.from(requireUser(id)));
    }

    @PostMapping
    public ApiResponse<AdminUserResponse> create(@RequestBody CreateAdminUserRequest req) {
        if (adminUserRepo.existsByKeycloakUserId(req.keycloakUserId())) {
            throw FixPayException.conflict("Admin user already registered: " + req.keycloakUserId());
        }
        AdminUser.AdminRole role;
        try {
            role = AdminUser.AdminRole.valueOf(req.role());
        } catch (IllegalArgumentException e) {
            throw FixPayException.badRequest("Invalid role: " + req.role());
        }
        AdminUser user = new AdminUser(req.keycloakUserId(), req.username(), req.email(), role, req.tenantScope());
        return ApiResponse.ok("Created", AdminUserResponse.from(adminUserRepo.save(user)));
    }

    @PatchMapping("/{id}/role")
    public ApiResponse<AdminUserResponse> changeRole(
            @PathVariable UUID id,
            @RequestBody java.util.Map<String, String> body) {

        AdminUser user = requireUser(id);
        String newRole = body.get("role");
        try {
            user.setRole(AdminUser.AdminRole.valueOf(newRole));
        } catch (IllegalArgumentException e) {
            throw FixPayException.badRequest("Invalid role: " + newRole);
        }
        return ApiResponse.ok("Role updated", AdminUserResponse.from(adminUserRepo.save(user)));
    }

    @PatchMapping("/{id}/deactivate")
    public ApiResponse<AdminUserResponse> deactivate(@PathVariable UUID id) {
        AdminUser user = requireUser(id);
        user.setActive(false);
        return ApiResponse.ok("Deactivated", AdminUserResponse.from(adminUserRepo.save(user)));
    }

    @PatchMapping("/{id}/activate")
    public ApiResponse<AdminUserResponse> activate(@PathVariable UUID id) {
        AdminUser user = requireUser(id);
        user.setActive(true);
        return ApiResponse.ok("Activated", AdminUserResponse.from(adminUserRepo.save(user)));
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private AdminUser requireUser(UUID id) {
        return adminUserRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("Admin user " + id));
    }
}
