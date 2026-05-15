package ng.fixpay.core.portal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import ng.fixpay.core.portal.domain.ApiKey;
import ng.fixpay.core.portal.domain.ApiKeyRepository;
import ng.fixpay.core.portal.domain.IpWhitelistRule;
import ng.fixpay.core.portal.domain.IpWhitelistRuleRepository;
import ng.fixpay.core.tenant.domain.Tenant;
import ng.fixpay.core.tenant.domain.TenantRepository;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Processes {@code X-API-Key} header on {@code /api/v1/**} routes.
 * <ol>
 *   <li>Hashes the provided key with SHA-256.</li>
 *   <li>Looks up the hash in {@code api_keys}.</li>
 *   <li>Validates: not revoked/expired, tenant status allows the environment.</li>
 *   <li>Enforces IP whitelist if any active rules exist for this tenant + environment.</li>
 *   <li>Injects {@link TenantApiKeyAuthentication} into the {@link SecurityContextHolder}.</li>
 * </ol>
 * Runs before {@code BearerTokenAuthenticationFilter}. If no API key header is present,
 * the filter is a no-op and normal JWT auth proceeds.
 */
@Component
public class ApiKeyAuthFilter extends OncePerRequestFilter {

    static final String HEADER = "X-API-Key";

    private final ApiKeyRepository        apiKeyRepository;
    private final IpWhitelistRuleRepository ipWhitelistRepository;
    private final TenantRepository        tenantRepository;

    public ApiKeyAuthFilter(ApiKeyRepository apiKeyRepository,
                             IpWhitelistRuleRepository ipWhitelistRepository,
                             TenantRepository tenantRepository) {
        this.apiKeyRepository     = apiKeyRepository;
        this.ipWhitelistRepository = ipWhitelistRepository;
        this.tenantRepository     = tenantRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Only process /api/v1/** requests that carry the header
        String path = request.getRequestURI();
        return !path.startsWith("/api/v1/") || request.getHeader(HEADER) == null;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {

        String rawKey = request.getHeader(HEADER);
        String hash   = sha256Hex(rawKey);

        Optional<ApiKey> keyOpt = apiKeyRepository.findByKeyHash(hash);
        if (keyOpt.isEmpty() || !keyOpt.get().isActive()) {
            rejectUnauthorized(response, "Invalid or revoked API key");
            return;
        }

        ApiKey apiKey = keyOpt.get();
        apiKey.markUsed();
        apiKeyRepository.save(apiKey);

        // Live key enforcement: tenant must be ACTIVE
        if (apiKey.getEnvironment() == ApiKey.Environment.LIVE) {
            Optional<Tenant> tenantOpt = tenantRepository.findById(apiKey.getTenantId());
            if (tenantOpt.isEmpty() || tenantOpt.get().getStatus() != Tenant.Status.ACTIVE) {
                rejectUnauthorized(response, "LIVE API keys require an active tenant account");
                return;
            }
        }

        // IP whitelist enforcement
        IpWhitelistRule.Environment env = apiKey.getEnvironment() == ApiKey.Environment.LIVE
                ? IpWhitelistRule.Environment.LIVE
                : IpWhitelistRule.Environment.SANDBOX;

        List<IpWhitelistRule> rules = ipWhitelistRepository
                .findByTenantIdAndEnvironmentAndActiveTrue(apiKey.getTenantId(), env);

        if (!rules.isEmpty()) {
            String remoteAddr = getClientIp(request);
            boolean allowed = rules.stream().anyMatch(rule -> ipMatches(remoteAddr, rule.getIpCidr()));
            if (!allowed) {
                rejectForbidden(response, "Request IP not in whitelist");
                return;
            }
        }

        // Inject authentication
        TenantApiKeyAuthentication auth = new TenantApiKeyAuthentication(
                apiKey.getTenantId(), apiKey.getId(), apiKey.getEnvironment(), apiKey.getScopes());
        SecurityContextHolder.getContext().setAuthentication(auth);

        chain.doFilter(request, response);
    }

    // ─── Utilities ────────────────────────────────────────────────────────────

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * Very lightweight CIDR check (IPv4 only for now).
     * For production, consider using a proper library like Apache Commons Net.
     */
    static boolean ipMatches(String ip, String cidr) {
        if (!cidr.contains("/")) {
            return ip.equals(cidr);
        }
        try {
            String[] parts = cidr.split("/");
            int    prefix = Integer.parseInt(parts[1]);
            long   mask   = prefix == 0 ? 0L : (0xFFFFFFFFL << (32 - prefix)) & 0xFFFFFFFFL;
            long   network = ipToLong(parts[0]) & mask;
            long   addr    = ipToLong(ip)       & mask;
            return network == addr;
        } catch (Exception e) {
            return false;
        }
    }

    private static long ipToLong(String ip) {
        String[] octets = ip.split("\\.");
        if (octets.length != 4) throw new IllegalArgumentException("Invalid IPv4: " + ip);
        long result = 0;
        for (String octet : octets) {
            result = (result << 8) | (Integer.parseInt(octet) & 0xFF);
        }
        return result;
    }

    private static void rejectUnauthorized(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\",\"code\":\"AUTH_001\"}");
    }

    private static void rejectForbidden(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType("application/json");
        response.getWriter().write("{\"success\":false,\"error\":\"" + message + "\",\"code\":\"AUTH_003\"}");
    }

    // ─── Authentication token ─────────────────────────────────────────────────

    public static class TenantApiKeyAuthentication extends AbstractAuthenticationToken {
        private final UUID              tenantId;
        private final UUID              apiKeyId;
        private final ApiKey.Environment environment;

        public TenantApiKeyAuthentication(UUID tenantId, UUID apiKeyId,
                                           ApiKey.Environment environment, List<String> scopes) {
            super(scopes.stream()
                    .map(s -> new SimpleGrantedAuthority("SCOPE_" + s))
                    .toList());
            this.tenantId    = tenantId;
            this.apiKeyId    = apiKeyId;
            this.environment = environment;
            setAuthenticated(true);
        }

        @Override public Object getCredentials() { return null; }
        @Override public Object getPrincipal()   { return tenantId.toString(); }

        public UUID              getTenantId()    { return tenantId; }
        public UUID              getApiKeyId()    { return apiKeyId; }
        public ApiKey.Environment getEnvironment() { return environment; }
    }
}

