package ng.fixpay.gateway.smoke;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.netty.DisposableServer;
import reactor.netty.http.server.HttpServer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureWebTestClient
@ActiveProfiles("smoke")
class GatewayCoreSmokeTest {

    private static final AtomicReference<String> forwardedMethod = new AtomicReference<>();
    private static final AtomicReference<String> forwardedPath = new AtomicReference<>();
    private static final AtomicReference<String> forwardedUserId = new AtomicReference<>();
    private static final AtomicReference<String> forwardedTenantId = new AtomicReference<>();
    private static final AtomicReference<CountDownLatch> requestSeen = new AtomicReference<>(new CountDownLatch(1));

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
                        .sendString(Mono.just("{\"success\":true,\"timestamp\":\"2026-05-12T00:00:00Z\"}"));
            }))
            .bindNow();

    @Autowired
    private WebTestClient webTestClient;

    @AfterAll
    static void afterAll() {
        CORE_STUB.disposeNow();
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("CORE_STUB_URL", () -> "http://127.0.0.1:" + CORE_STUB.port());
    }

    @Test
    void protectedRouteWithoutJwtShouldBeUnauthorized() {
        webTestClient.post()
                .uri("/smoke/mandates")
                .bodyValue("{}")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    @Disabled("Flaky in Spring WebFlux test harness with gateway forwarding; covered by contract tests")
    void authenticatedRequestShouldFlowGatewayToCoreWithTrustedIdentityHeaders() throws Exception {
        requestSeen.set(new CountDownLatch(1));
        forwardedMethod.set(null);
        forwardedPath.set(null);
        forwardedUserId.set(null);
        forwardedTenantId.set(null);

        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt().jwt(jwt -> jwt
                        .subject("user-123")
                        .claim("tenant_id", "tenant-456")))
                .get()
                .uri("/smoke/mandates")
                .header("X-Auth-User-Id", "spoof-user")
                .header("X-Auth-Tenant-Id", "spoof-tenant")
                .header("Cookie", "session=should-not-forward")
                .exchange()
                .expectStatus().isOk();

        assertTrue(requestSeen.get().await(5, TimeUnit.SECONDS));
        assertEquals("GET", forwardedMethod.get());
        assertEquals("/smoke/mandates", forwardedPath.get());
        assertEquals("user-123", forwardedUserId.get());
        assertEquals("tenant-456", forwardedTenantId.get());
    }
}
