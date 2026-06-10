package ng.fixpay.core.portal;

import ng.fixpay.shared.exception.FixPayException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Keycloak admin operations scoped to the {@code tenants} realm.
 * Used for self-service portal registration and management.
 */
@Component
public class TenantKeycloakClient {

    private static final Logger log = LoggerFactory.getLogger(TenantKeycloakClient.class);

    private final RestClient rest;
    private final String kcUrl;
    private final String tenantRealm;
    private final String adminRealm;
    private final String adminClient;
    private final String adminUser;
    private final String adminPassword;

    public TenantKeycloakClient(
            @Value("${fixpay.keycloak.url}")             String kcUrl,
            @Value("${fixpay.keycloak.tenant-realm}")    String tenantRealm,
            @Value("${fixpay.keycloak.admin-realm}")     String adminRealm,
            @Value("${fixpay.keycloak.admin-client}")    String adminClient,
            @Value("${fixpay.keycloak.admin-user}")      String adminUser,
            @Value("${fixpay.keycloak.admin-password}")  String adminPassword
    ) {
        this.kcUrl         = kcUrl;
        this.tenantRealm   = tenantRealm;
        this.adminRealm    = adminRealm;
        this.adminClient   = adminClient;
        this.adminUser     = adminUser;
        this.adminPassword = adminPassword;
        this.rest          = RestClient.create();
    }

    /** Obtain a master-realm admin token. */
    @SuppressWarnings("unchecked")
    private String adminToken() {
        String tokenUrl = "%s/realms/%s/protocol/openid-connect/token"
                .formatted(kcUrl, adminRealm);
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "password");
        form.add("client_id",  adminClient);
        form.add("username",   adminUser);
        form.add("password",   adminPassword);

        Map<String, Object> response = rest.post()
                .uri(tokenUrl)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(Map.class);

        if (response == null || !response.containsKey("access_token")) {
            throw FixPayException.badRequest("Keycloak admin auth failed");
        }
        return (String) response.get("access_token");
    }

    /**
     * Create a new TENANT_ADMIN user in the {@code tenants} realm.
     *
     * @param email       login email (used as both username and email)
     * @param password    initial password
     * @param tenantId    tenant UUID written into user attributes
     * @param businessName display name
     * @return Keycloak user UUID
     */
    public UUID createTenantAdmin(String email, String password, UUID tenantId, String businessName) {
        String token    = adminToken();
        String usersUrl = "%s/admin/realms/%s/users".formatted(kcUrl, tenantRealm);

        Map<String, Object> credential = Map.of(
                "type",      "password",
                "value",     password,
                "temporary", false
        );

        Map<String, Object> userRep = new java.util.HashMap<>();
        userRep.put("username",      email);
        userRep.put("email",         email);
        userRep.put("firstName",     businessName);
        userRep.put("enabled",       true);
        userRep.put("emailVerified", false);          // user should verify later
        userRep.put("requiredActions", List.of());
        userRep.put("credentials", List.of(credential));
        userRep.put("attributes", Map.of(
                "tenant_id", List.of(tenantId.toString())
        ));

        ResponseEntity<Void> response;
        try {
            response = rest.post()
                    .uri(usersUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(userRep)
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException.Conflict e) {
            throw FixPayException.conflict("Email address already registered");
        }

        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null) {
            throw FixPayException.badRequest("Keycloak did not return user location");
        }
        String keycloakId = location.substring(location.lastIndexOf('/') + 1);

        // Assign TENANT_ADMIN role
        assignRealmRole(token, keycloakId, "TENANT_ADMIN");

        log.info("Created portal user {} in realm {}", keycloakId, tenantRealm);
        return UUID.fromString(keycloakId);
    }

    /** Assign a realm-level role to a user in the tenants realm. */
    @SuppressWarnings("unchecked")
    private void assignRealmRole(String token, String keycloakUserId, String roleName) {
        String rolesUrl = "%s/admin/realms/%s/roles/%s".formatted(kcUrl, tenantRealm, roleName);

        Map<String, Object> roleRep = rest.get()
                .uri(rolesUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .retrieve()
                .body(Map.class);

        if (roleRep == null) {
            log.warn("Role {} not found in realm {}, skipping assignment", roleName, tenantRealm);
            return;
        }

        String assignUrl = "%s/admin/realms/%s/users/%s/role-mappings/realm"
                .formatted(kcUrl, tenantRealm, keycloakUserId);

        rest.post()
                .uri(assignUrl)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .body(List.of(roleRep))
                .retrieve()
                .toBodilessEntity();
    }
}

