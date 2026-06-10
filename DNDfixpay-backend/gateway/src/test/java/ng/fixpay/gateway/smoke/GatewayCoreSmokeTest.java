package ng.fixpay.gateway.smoke;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Smoke test verifying the gateway: rejects unauthenticated requests,
 * strips spoofed identity headers, and forwards JWT-derived headers downstream.
 *
 * Uses {@code @MockBean ReactiveJwtDecoder} (no real Keycloak/JWK needed) and
 * {@code WebTestClient.bindToServer()} over real HTTP so Spring Cloud Gateway's
 * Netty routing filter can proxy requests without an inbound-receiver conflict.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("smoke")
class GatewayCoreSmokeTest {

    /** Replaces the NimbusReactiveJwtDecoder auto-configured bean. */
    @MockBean
    private ReactiveJwtDecoder jwtDecoder;

    @LocalServerPort
    private int gatewayPort;

    private static final AtomicReference<String> forwardedMethod   = new AtomicReference<>();
    private static final AtomicReference<String> forwardedPath     = new AtomicReference<>();
    private static final AtomicReference<String> forwardedUserId   = new AtomicReference<>();
    private static final AtomicReference<String> forwardedTenantId = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch> requestSeen =
            new AtomicReference<>(new CountDownLatch(1));

    /** Reactor Netty stub that captures headers forwarded by the gateway. */
    private static final DisposableServer CORE_STUB = HttpServer.create()
            .host("127.0.0.1")
            .port(0)
            .route(routes -> routes.route(req -> true, (req, res) -> {
                forwardedMethod.set(req.method().name());
                forwardedPath.set(req.uri());
                forwardedUserId.set(req.requestHeaders().get("X-Auth-User-Id"));
                forwardedTenantId.set(req.requestHeaders().get("X-Auth-Tenant-Id"));
                requestSeen.get().countDown();
                return res.status(200)
                        .header("Content-Type", "application/json")
                        .sendString(Mono.just("{\"ok\":true}"));
            }))
            .bindNow();

    /** Real HTTP client — bypasses in-process mock connector. */
    private WebTestClient client;

    @AfterAll
    static void afterAll() {
        CORE_STUB.disposeNow();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("CORE_STUB_URL", () -> "http://127.0.0.1:" + CORE_STUB.port());
    }

    @BeforeEach
    void setup() {
        client = WebTestClient.bindToServer()
                .baseUrl("http://localhost:" + gatewayPort)
                .responseTimeout(java.time.Duration.ofSeconds(10))
                .build();

        Jwt jwt = Jwt.withTokenValue("test-token")
                .header("alg", "RS256")
                .subject("user-123")
                .claim("tenant_id", "tenant-456")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        when(jwtDecoder.decode(anyString())).thenReturn(Mono.just(jwt));
    }

    @Test
    void protectedRouteWithoutJwtShouldBeUnauthorized() {
        client.post()
                .uri("/smoke/mandates")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void authenticatedRequestShouldFlowGatewayToCoreWithTrustedIdentityHeaders() throws Exception {
        requestSeen.set(new CountDownLatch(1));
        forwardedMethod.set(null);
        forwardedPath.set(null);
        forwardedUserId.set(null);
        forwardedTenantId.set(null);

        client.get()
                .uri("/smoke/mandates")
                .header("Authorization",    "Bearer test-token")
                .header("X-Auth-User-Id",   "spoof-user")
                .header("X-Auth-Tenant-Id", "spoof-tenant")
                .header("Cookie",           "session=should-not-forward")
                .exchange()
                .expectStatus().isOk();

        assertTrue(requestSeen.get().await(5, TimeUnit.SECONDS),
                "CORE_STUB did not receive a forwarded request within 5 s");
        assertEquals("GET",             forwardedMethod.get());
        assertEquals("/smoke/mandates", forwardedPath.get());
        assertEquals("user-123",        forwardedUserId.get(),
                "X-Auth-User-Id must be replaced with JWT sub");
        assertEquals("tenant-456",      forwardedTenantId.get(),
                "X-Auth-Tenant-Id must be replaced with JWT tenant_id claim");
    }
}
