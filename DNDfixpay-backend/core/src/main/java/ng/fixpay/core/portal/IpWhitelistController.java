package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import ng.fixpay.core.portal.domain.IpWhitelistRule;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/ip-whitelist")
public class IpWhitelistController {

    private final IpWhitelistRuleRepository ipRepo;
    private final TenantPortalController    portalHelper;

    public IpWhitelistController(IpWhitelistRuleRepository ipRepo,
                                   TenantPortalController portalHelper) {
        this.ipRepo       = ipRepo;
        this.portalHelper = portalHelper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<IpWhitelistRule>>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String environment) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        List<IpWhitelistRule> rules = environment != null
                ? ipRepo.findByTenantIdAndEnvironmentAndActiveTrue(
                        tenant.getId(), IpWhitelistRule.Environment.valueOf(environment.toUpperCase()))
                : ipRepo.findByTenantIdOrderByCreatedAtDesc(tenant.getId());

        return ResponseEntity.ok(ApiResponse.ok(rules));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<IpWhitelistRule>> add(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody AddRuleRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        IpWhitelistRule.Environment env =
                IpWhitelistRule.Environment.valueOf(req.environment().toUpperCase());

        // LIVE rules: tenant must be ACTIVE
        if (env == IpWhitelistRule.Environment.LIVE && tenant.getStatus() != Tenant.Status.ACTIVE) {
            throw FixPayException.conflict("LIVE IP rules require an active tenant account");
        }

        IpWhitelistRule rule = new IpWhitelistRule(tenant.getId(), req.ipCidr(), req.label(), env);
        return ResponseEntity.ok(ApiResponse.ok(ipRepo.save(rule)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        IpWhitelistRule rule = ipRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("IP whitelist rule not found"));

        if (!rule.getTenantId().equals(tenant.getId())) {
            throw FixPayException.forbidden("Not your IP rule");
        }
        rule.setActive(false);
        ipRepo.save(rule);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    public record AddRuleRequest(
            @NotBlank
            @Pattern(regexp = "^(\\d{1,3}\\.){3}\\d{1,3}(/\\d{1,2})?$",
                     message = "Must be a valid IPv4 address or CIDR block")
            String ipCidr,

            @Size(max = 120) String label,
            @NotBlank        String environment
    ) {}
}
