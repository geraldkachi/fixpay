package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import ng.fixpay.core.portal.domain.WebhookEndpoint;
import ng.fixpay.core.portal.domain.WebhookEndpointRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/webhooks")
public class WebhookController {

    private static final SecureRandom RNG = new SecureRandom();

    private final WebhookEndpointRepository webhookRepo;
    private final TenantPortalController    portalHelper;

    public WebhookController(WebhookEndpointRepository webhookRepo,
                              TenantPortalController portalHelper) {
        this.webhookRepo  = webhookRepo;
        this.portalHelper = portalHelper;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<WebhookEndpoint>>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String environment) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        List<WebhookEndpoint> endpoints = environment != null
                ? webhookRepo.findByTenantIdAndEnvironment(
                        tenant.getId(), WebhookEndpoint.Environment.valueOf(environment.toUpperCase()))
                : webhookRepo.findByTenantIdOrderByCreatedAtDesc(tenant.getId());

        return ResponseEntity.ok(ApiResponse.ok(endpoints));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<WebhookEndpoint>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateWebhookRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        WebhookEndpoint.Environment env =
                WebhookEndpoint.Environment.valueOf(req.environment().toUpperCase());

        // LIVE webhooks: tenant must be ACTIVE
        if (env == WebhookEndpoint.Environment.LIVE && tenant.getStatus() != Tenant.Status.ACTIVE) {
            throw FixPayException.conflict("LIVE webhooks require an active tenant account");
        }

        String signingSecret = "whsec_" + randomHex(32);
        WebhookEndpoint endpoint = new WebhookEndpoint(
                tenant.getId(), req.url(), req.events(), signingSecret, env);
        return ResponseEntity.ok(ApiResponse.ok(webhookRepo.save(endpoint)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<WebhookEndpoint>> update(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestBody UpdateWebhookRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        WebhookEndpoint endpoint = webhookRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("Webhook endpoint not found"));
        if (!endpoint.getTenantId().equals(tenant.getId())) {
            throw FixPayException.forbidden("Not your webhook");
        }
        if (req.url()    != null) endpoint.setUrl(req.url());
        if (req.events() != null) endpoint.setEvents(req.events());
        if (req.active() != null) endpoint.setActive(req.active());
        return ResponseEntity.ok(ApiResponse.ok(webhookRepo.save(endpoint)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        WebhookEndpoint endpoint = webhookRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("Webhook endpoint not found"));
        if (!endpoint.getTenantId().equals(tenant.getId())) {
            throw FixPayException.forbidden("Not your webhook");
        }
        webhookRepo.delete(endpoint);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes / 2];
        RNG.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    public record CreateWebhookRequest(
            @NotBlank @Size(max = 500) String url,
            @NotEmpty List<String>           events,
            @NotBlank                  String environment
    ) {}

    public record UpdateWebhookRequest(String url, List<String> events, Boolean active) {}
}

