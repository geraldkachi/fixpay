package ng.fixpay.core.portal;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import ng.fixpay.core.portal.domain.ApiKey;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/portal/api-keys")
public class ApiKeyController {

    private static final SecureRandom RNG = new SecureRandom();

    private final ApiKeyRepository       apiKeyRepo;
    private final TenantPortalController portalHelper;

    public ApiKeyController(ApiKeyRepository apiKeyRepo, TenantPortalController portalHelper) {
        this.apiKeyRepo   = apiKeyRepo;
        this.portalHelper = portalHelper;
    }

    /** GET /api/portal/api-keys — list all keys for this tenant (hashes NOT returned) */
    @GetMapping
    public ResponseEntity<ApiResponse<List<ApiKeyResponse>>> list(
            @AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String environment) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        List<ApiKey> keys = environment != null
                ? apiKeyRepo.findByTenantIdAndEnvironmentOrderByCreatedAtDesc(
                        tenant.getId(), ApiKey.Environment.valueOf(environment.toUpperCase()))
                : apiKeyRepo.findByTenantIdOrderByCreatedAtDesc(tenant.getId());

        return ResponseEntity.ok(ApiResponse.ok(keys.stream().map(ApiKeyResponse::from).toList()));
    }

    /** POST /api/portal/api-keys — generate a new API key (raw key returned exactly once) */
    @PostMapping
    public ResponseEntity<ApiResponse<CreateApiKeyResponse>> create(
            @AuthenticationPrincipal Jwt jwt,
            @Valid @RequestBody CreateApiKeyRequest req) {

        Tenant tenant = portalHelper.resolveTenant(jwt);

        // LIVE keys: tenant must be ACTIVE
        ApiKey.Environment env = ApiKey.Environment.valueOf(req.environment().toUpperCase());
        if (env == ApiKey.Environment.LIVE && tenant.getStatus() != Tenant.Status.ACTIVE) {
            throw FixPayException.conflict("LIVE API keys require the tenant account to be active (approved by platform)");
        }

        // Generate the raw key: fpk_test_<32 hex chars> or fpk_live_<32 hex chars>
        String envLabel = env == ApiKey.Environment.LIVE ? "live" : "test";
        String rawKey   = "fpk_" + envLabel + "_" + randomHex(32);
        String keyHash  = sha256Hex(rawKey);
        String keyPrefix = rawKey.substring(0, rawKey.indexOf('_', 4) + 5); // "fpk_test_" or "fpk_live_"

        UUID callerId = UUID.fromString(jwt.getSubject());
        ApiKey apiKey = new ApiKey(
                tenant.getId(), req.name(), env,
                keyPrefix, keyHash, req.scopes() != null ? req.scopes() : List.of(),
                callerId
        );
        apiKeyRepo.save(apiKey);

        // Return the raw key ONCE — it cannot be retrieved again
        return ResponseEntity.ok(ApiResponse.ok(
                new CreateApiKeyResponse(ApiKeyResponse.from(apiKey), rawKey)));
    }

    /** DELETE /api/portal/api-keys/{id} — revoke a key */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> revoke(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id) {

        Tenant tenant = portalHelper.resolveTenant(jwt);
        ApiKey apiKey = apiKeyRepo.findById(id)
                .orElseThrow(() -> FixPayException.notFound("API key not found"));

        if (!apiKey.getTenantId().equals(tenant.getId())) {
            throw FixPayException.forbidden("Not your API key");
        }
        apiKey.revoke();
        apiKeyRepo.save(apiKey);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private static String randomHex(int bytes) {
        byte[] buf = new byte[bytes / 2];
        RNG.nextBytes(buf);
        return HexFormat.of().formatHex(buf);
    }

    static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    // ─── DTOs ─────────────────────────────────────────────────────────────────

    public record CreateApiKeyRequest(
            @NotBlank @Size(min = 1, max = 120) String name,
            @NotBlank                           String environment,
            List<String>                        scopes
    ) {}

    public record ApiKeyResponse(
            UUID              id,
            String            name,
            String            environment,
            String            keyPrefix,
            List<String>      scopes,
            java.time.Instant lastUsedAt,
            java.time.Instant expiresAt,
            java.time.Instant revokedAt,
            java.time.Instant createdAt
    ) {
        static ApiKeyResponse from(ApiKey k) {
            return new ApiKeyResponse(k.getId(), k.getName(), k.getEnvironment().name(),
                    k.getKeyPrefix(), k.getScopes(), k.getLastUsedAt(),
                    k.getExpiresAt(), k.getRevokedAt(), k.getCreatedAt());
        }
    }

    public record CreateApiKeyResponse(ApiKeyResponse key, String rawKey) {}
}

