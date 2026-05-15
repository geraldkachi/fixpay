package ng.fixpay.core.config;

import ng.fixpay.core.portal.ApiKeyAuthFilter;
import ng.fixpay.shared.dto.ApiResponse;
import ng.fixpay.shared.exception.FixPayException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoders;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationProvider;
import org.springframework.security.oauth2.server.resource.authentication.JwtIssuerAuthenticationManagerResolver;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper;
    private final ApiKeyAuthFilter apiKeyAuthFilter;
    private final String keycloakUrl;
    private final ConcurrentMap<String, AuthenticationManager> issuerAuthenticationManagers = new ConcurrentHashMap<>();

    public SecurityConfig(
            ObjectMapper objectMapper,
            ApiKeyAuthFilter apiKeyAuthFilter,
            @Value("${fixpay.keycloak.url:http://localhost:8180}") String keycloakUrl
    ) {
        this.objectMapper = objectMapper;
        this.apiKeyAuthFilter = apiKeyAuthFilter;
        this.keycloakUrl = keycloakUrl;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Public endpoints
                .requestMatchers(HttpMethod.GET,  "/api/tenant/config").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/verify-otp").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/refresh").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/logout").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/payments/vtpass/webhook").permitAll()
                .requestMatchers("/api/health", "/api/version").permitAll()
                .requestMatchers("/actuator/health", "/actuator/info").permitAll()
                // Self-service portal registration — fully public
                .requestMatchers(HttpMethod.POST, "/api/portal/register").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/portal/check-slug").permitAll()
                // Admin endpoints — require PLATFORM_ADMIN Keycloak realm role
                .requestMatchers("/api/admin/transactions/**").hasAnyAuthority("PLATFORM_ADMIN", "FINANCE_OPS")
                .requestMatchers("/api/admin/**").hasAuthority("PLATFORM_ADMIN")
                // Portal endpoints — require TENANT_ADMIN or TENANT_STAFF
                .requestMatchers("/api/portal/**").hasAnyAuthority("TENANT_ADMIN", "TENANT_STAFF")
                // API v1 — accepts either JWT or API key (handled by ApiKeyAuthFilter)
                .requestMatchers("/api/v1/**").authenticated()
                // Everything else requires a valid JWT
                .anyRequest().authenticated()
            )
            .oauth2ResourceServer(oauth2 -> oauth2
                .authenticationManagerResolver(keycloakIssuerResolver())
                .authenticationEntryPoint((request, response, ex) -> {
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    var body = ApiResponse.error("Unauthorized", "AUTH_001");
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
                .accessDeniedHandler((request, response, ex) -> {
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    var body = ApiResponse.error("Forbidden", "AUTH_002");
                    response.getWriter().write(objectMapper.writeValueAsString(body));
                })
            );

        // API key filter runs before the OAuth2 bearer token filter
        http.addFilterBefore(apiKeyAuthFilter, BearerTokenAuthenticationFilter.class);

        return http.build();
    }

    /**
     * Maps Keycloak {@code realm_access.roles} claim to Spring {@link GrantedAuthority} instances.
     * Role names are used as-is (no prefix), so {@code hasAuthority("PLATFORM_ADMIN")} works directly.
     */
    @Bean
    public JwtAuthenticationConverter keycloakJwtConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRolesConverter());
        return converter;
    }

    @Bean
    public JwtIssuerAuthenticationManagerResolver keycloakIssuerResolver() {
        List<String> issuers = trustedIssuers();
        return new JwtIssuerAuthenticationManagerResolver(issuer -> {
            if (!issuers.contains(issuer)) {
                return null;
            }
            return issuerAuthenticationManagers.computeIfAbsent(issuer, this::buildAuthenticationManager);
        });
    }

    private AuthenticationManager buildAuthenticationManager(String issuer) {
        JwtAuthenticationProvider provider = new JwtAuthenticationProvider(JwtDecoders.fromIssuerLocation(issuer));
        provider.setJwtAuthenticationConverter(keycloakJwtConverter());
        return provider::authenticate;
    }

    private List<String> trustedIssuers() {
        String base = keycloakUrl.endsWith("/") ? keycloakUrl.substring(0, keycloakUrl.length() - 1) : keycloakUrl;
        return List.of(
                base + "/realms/customers",
                base + "/realms/tenants",
                base + "/realms/platform"
        );
    }

    /** Extracts {@code realm_access.roles} from a Keycloak JWT and maps them to authorities. */
    static class KeycloakRealmRolesConverter implements Converter<Jwt, Collection<GrantedAuthority>> {
        @Override
        @SuppressWarnings("unchecked")
        public Collection<GrantedAuthority> convert(Jwt jwt) {
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
            if (realmAccess == null) return List.of();
            Object rolesObj = realmAccess.get("roles");
            if (!(rolesObj instanceof List<?> roles)) return List.of();
            return roles.stream()
                    .filter(r -> r instanceof String)
                    .map(r -> (GrantedAuthority) new SimpleGrantedAuthority((String) r))
                    .toList();
        }
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(
            "https://*.surge.sh",
            "https://*.trycloudflare.com",
            "http://localhost:*"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
