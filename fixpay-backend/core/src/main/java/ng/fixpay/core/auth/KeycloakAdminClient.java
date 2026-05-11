package ng.fixpay.core.auth;

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

@Component
public class KeycloakAdminClient {

    private static final Logger log = LoggerFactory.getLogger(KeycloakAdminClient.class);

    private final RestClient rest;
    private final String kcUrl;
    private final String kcRealm;
    private final String adminRealm;
    private final String adminClient;
    private final String adminUser;
    private final String adminPassword;

    public KeycloakAdminClient(
            @Value("${fixpay.keycloak.url}")            String kcUrl,
            @Value("${fixpay.keycloak.realm}")          String kcRealm,
            @Value("${fixpay.keycloak.admin-realm}")    String adminRealm,
            @Value("${fixpay.keycloak.admin-client}")   String adminClient,
            @Value("${fixpay.keycloak.admin-user}")     String adminUser,
            @Value("${fixpay.keycloak.admin-password}") String adminPassword
    ) {
        this.kcUrl         = kcUrl;
        this.kcRealm       = kcRealm;
        this.adminRealm    = adminRealm;
        this.adminClient   = adminClient;
        this.adminUser     = adminUser;
        this.adminPassword = adminPassword;
        this.rest          = RestClient.create();
    }

    /** Obtain an admin access token from the master realm. */
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
     * Create a user in the customers realm.
     * @return the Keycloak user UUID (the "sub" claim)
     */
    public UUID createUser(String phone, String email, String password, String tenantId) {
        String token   = adminToken();
        String usersUrl = "%s/admin/realms/%s/users".formatted(kcUrl, kcRealm);

        Map<String, Object> credential = Map.of(
                "type",      "password",
                "value",     password,
                "temporary", false
        );

        Map<String, Object> userRep = new java.util.HashMap<>();
        userRep.put("username",   phone);
        userRep.put("enabled",    true);
        userRep.put("credentials", List.of(credential));
        if (email != null && !email.isBlank()) {
            userRep.put("email", email);
        }
        userRep.put("attributes", Map.of(
                "tenant_id",  List.of(tenantId),
                "kyc_status", List.of("pending")
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
            throw FixPayException.conflict("Phone number already registered");
        }

        // Keycloak returns the new user URL in Location header
        String location = response.getHeaders().getFirst(HttpHeaders.LOCATION);
        if (location == null) {
            throw FixPayException.badRequest("Keycloak did not return user location");
        }
        String keycloakId = location.substring(location.lastIndexOf('/') + 1);
        log.info("Created Keycloak user {} in realm {}", keycloakId, kcRealm);
        return UUID.fromString(keycloakId);
    }
}
