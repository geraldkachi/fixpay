package ng.fixpay.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class UserContextRelayFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerWebExchange sanitized = sanitizeSpoofedHeaders(exchange);

        return ReactiveSecurityContextHolder.getContext()
                .map(ctx -> ctx.getAuthentication())
                .filter(JwtAuthenticationToken.class::isInstance)
                .cast(JwtAuthenticationToken.class)
                .flatMap(jwtAuth -> {
                    String userId = jwtAuth.getToken().getSubject();
                    String tenantId = jwtAuth.getToken().getClaimAsString("tenant_id");

                    var requestBuilder = sanitized.getRequest().mutate();
                    requestBuilder.headers(headers -> {
                        headers.add("X-Auth-User-Id", userId);
                        if (tenantId != null && !tenantId.isBlank()) {
                            headers.add("X-Auth-Tenant-Id", tenantId);
                        }
                    });

                    return chain.filter(sanitized.mutate().request(requestBuilder.build()).build());
                })
                .switchIfEmpty(chain.filter(sanitized));
    }

    private ServerWebExchange sanitizeSpoofedHeaders(ServerWebExchange exchange) {
        var requestBuilder = exchange.getRequest().mutate();
        requestBuilder.headers(headers -> {
            headers.remove("X-Auth-User-Id");
            headers.remove("X-Auth-Tenant-Id");
        });
        return exchange.mutate().request(requestBuilder.build()).build();
    }

    @Override
    public int getOrder() {
        return -50;
    }
}
