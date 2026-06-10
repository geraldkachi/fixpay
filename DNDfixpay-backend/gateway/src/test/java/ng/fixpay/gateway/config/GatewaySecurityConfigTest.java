package ng.fixpay.gateway.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.reactive.server.SecurityMockServerConfigurers;
import org.springframework.test.web.reactive.server.WebTestClient;

@WebFluxTest
@Import(GatewaySecurityConfig.class)
class GatewaySecurityConfigTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void protectedRouteShouldReturnUnauthorizedWithoutJwt() {
        webTestClient.get()
                .uri("/api/protected/ping")
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void healthRouteShouldBePublic() {
        webTestClient.get()
                .uri("/api/health")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void webhookRouteShouldBePublic() {
        webTestClient.post()
                .uri("/api/payments/vtpass/webhook")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue("{\"paymentReference\":\"FP-VTP-123\",\"providerStatus\":\"pending\"}")
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void protectedRouteShouldAllowJwt() {
        webTestClient
                .mutateWith(SecurityMockServerConfigurers.mockJwt())
                .get()
                .uri("/api/protected/ping")
                .exchange()
                .expectStatus().isNotFound();
    }
}
