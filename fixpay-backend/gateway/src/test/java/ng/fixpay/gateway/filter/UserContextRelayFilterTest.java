package ng.fixpay.gateway.filter;

import org.junit.jupiter.api.Test;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class UserContextRelayFilterTest {

    private final UserContextRelayFilter filter = new UserContextRelayFilter();

    @Test
    void shouldRemoveSpoofedHeadersWhenNoAuthentication() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/protected")
                .header("X-Auth-User-Id", "spoof-user")
                .header("X-Auth-Tenant-Id", "spoof-tenant")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain).block();

        assertNull(chain.exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id"));
        assertNull(chain.exchange.getRequest().getHeaders().getFirst("X-Auth-Tenant-Id"));
    }

    @Test
    void shouldInjectJwtDerivedHeadersAndStripSpoofedValues() {
        MockServerHttpRequest request = MockServerHttpRequest.get("/api/protected")
                .header("X-Auth-User-Id", "spoof-user")
                .header("X-Auth-Tenant-Id", "spoof-tenant")
                .build();
        MockServerWebExchange exchange = MockServerWebExchange.from(request);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", "real-user")
                .claim("tenant_id", "real-tenant")
                .build();
        JwtAuthenticationToken authentication = new JwtAuthenticationToken(jwt);

        CapturingChain chain = new CapturingChain();
        filter.filter(exchange, chain)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                .block();

        assertEquals("real-user", chain.exchange.getRequest().getHeaders().getFirst("X-Auth-User-Id"));
        assertEquals("real-tenant", chain.exchange.getRequest().getHeaders().getFirst("X-Auth-Tenant-Id"));
    }

    private static class CapturingChain implements GatewayFilterChain {
        private ServerWebExchange exchange;

        @Override
        public Mono<Void> filter(ServerWebExchange exchange) {
            this.exchange = exchange;
            return Mono.empty();
        }
    }
}
