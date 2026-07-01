package com.ecommerce.gateway.filter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class AuthenticatedUserHeadersFilter implements GlobalFilter, Ordered {

    private static final List<String> IDENTITY_HEADERS = List.of(
            "X-User-Id",
            "X-User-Email",
            "X-User-Roles",
            "X-User-Scopes"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return exchange.getPrincipal()
                .ofType(JwtAuthenticationToken.class)
                .map(authentication -> withTrustedIdentityHeaders(exchange, authentication))
                .defaultIfEmpty(withoutIdentityHeaders(exchange))
                .flatMap(chain::filter);
    }

    @Override
    public int getOrder() {
        // Corre despues de Spring Security para usar el JWT ya validado.
        return Ordered.LOWEST_PRECEDENCE;
    }

    private ServerWebExchange withTrustedIdentityHeaders(
            ServerWebExchange exchange,
            JwtAuthenticationToken authentication
    ) {
        Jwt jwt = authentication.getToken();
        ServerHttpRequest request = stripIdentityHeaders(exchange.getRequest())
                .mutate()
                .headers(headers -> {
                    if (jwt.getSubject() != null) {
                        headers.set("X-User-Id", jwt.getSubject());
                    }

                    String email = jwt.getClaimAsString("email");
                    if (email != null && !email.isBlank()) {
                        headers.set("X-User-Email", email);
                    }

                    String roles = authentication.getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .filter(authority -> authority.startsWith("ROLE_"))
                            .map(authority -> authority.replaceFirst("^ROLE_", ""))
                            .distinct()
                            .collect(Collectors.joining(","));

                    if (!roles.isBlank()) {
                        headers.set("X-User-Roles", roles);
                    }

                    String scopes = authentication.getAuthorities().stream()
                            .map(authority -> authority.getAuthority())
                            .filter(authority -> authority.startsWith("SCOPE_"))
                            .map(authority -> authority.replaceFirst("^SCOPE_", ""))
                            .distinct()
                            .collect(Collectors.joining(","));

                    if (!scopes.isBlank()) {
                        headers.set("X-User-Scopes", scopes);
                    }
                })
                .build();

        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange withoutIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = stripIdentityHeaders(exchange.getRequest());
        return exchange.mutate().request(request).build();
    }

    private ServerHttpRequest stripIdentityHeaders(ServerHttpRequest request) {
        return request.mutate()
                .headers(headers -> IDENTITY_HEADERS.stream()
                        .filter(Objects::nonNull)
                        .forEach(headers::remove))
                .build();
    }
}
