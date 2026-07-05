package com.ecommerce.gateway.config;

import com.ecommerce.gateway.config.properties.JwtProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    private static final String CATALOG_READ = "SCOPE_catalog:read";
    private static final String CATALOG_WRITE = "SCOPE_catalog:write";
    private static final String STOCK_MANAGE = "SCOPE_stock:manage";
    private static final String CART_MANAGE = "SCOPE_cart:manage";
    private static final String CHECKOUT_CREATE = "SCOPE_checkout:create";
    private static final String ORDERS_READ_SELF = "SCOPE_orders:read:self";
    private static final String ORDERS_PAY_SELF = "SCOPE_orders:pay:self";
    private static final String ORDERS_CANCEL_SELF = "SCOPE_orders:cancel:self";
    private static final String USERS_READ_SELF = "SCOPE_users:read:self";

    @Bean
    SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http, ObjectMapper objectMapper) {
        return http
                // El gateway es stateless: no hay formularios, sesiones ni CSRF.
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .httpBasic(ServerHttpSecurity.HttpBasicSpec::disable)
                .formLogin(ServerHttpSecurity.FormLoginSpec::disable)
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint((exchange, exception) -> writeErrorResponse(
                                exchange,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                "Authentication required",
                                exception
                        ))
                        .accessDeniedHandler((exchange, exception) -> writeErrorResponse(
                                exchange,
                                objectMapper,
                                HttpStatus.FORBIDDEN,
                                "Forbidden",
                                exception
                        ))
                )
                .authorizeExchange(exchanges -> exchanges
                        // El identity-service debe permitir obtener tokens sin token previo.
                        .pathMatchers(HttpMethod.POST, "/api/auth/login", "/api/auth/register").permitAll()
                        // Health checks y documentacion no deben exigir JWT.
                        .pathMatchers("/actuator/health/**", "/actuator/info").permitAll()
                        .pathMatchers("/api/identity/health", "/api/catalog/health", "/api/commerce/health").permitAll()
                        .pathMatchers("/api/identity/v3/api-docs", "/api/catalog/v3/api-docs",
                                "/api/commerce/v3/api-docs").permitAll()
                        .pathMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/docs/**").permitAll()
                        // Rutas de negocio protegidas por scopes emitidos por identity-service.
                        .pathMatchers(HttpMethod.GET, "/api/products", "/api/products/**").hasAuthority(CATALOG_READ)
                        .pathMatchers(HttpMethod.GET, "/api/categories", "/api/categories/**").hasAuthority(CATALOG_READ)
                        .pathMatchers(HttpMethod.GET, "/api/brands", "/api/brands/**").hasAuthority(CATALOG_READ)
                        .pathMatchers(HttpMethod.POST, "/api/products", "/api/categories", "/api/brands")
                        .hasAuthority(CATALOG_WRITE)
                        .pathMatchers(HttpMethod.PATCH, "/api/products/*/stock").hasAuthority(STOCK_MANAGE)
                        .pathMatchers("/api/cart", "/api/cart/**").hasAuthority(CART_MANAGE)
                        .pathMatchers(HttpMethod.POST, "/api/checkout").hasAuthority(CHECKOUT_CREATE)
                        .pathMatchers(HttpMethod.GET, "/api/orders", "/api/orders/*").hasAuthority(ORDERS_READ_SELF)
                        .pathMatchers(HttpMethod.POST, "/api/orders/*/payment-confirmations")
                        .hasAuthority(ORDERS_PAY_SELF)
                        .pathMatchers(HttpMethod.POST, "/api/orders/*/cancellations").hasAuthority(ORDERS_CANCEL_SELF)
                        .pathMatchers(HttpMethod.GET, "/api/users/me").hasAuthority(USERS_READ_SELF)
                        // Todo lo demas entra al backend solo con JWT valido.
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .authenticationEntryPoint((exchange, exception) -> writeErrorResponse(
                                exchange,
                                objectMapper,
                                HttpStatus.UNAUTHORIZED,
                                "Authentication required",
                                exception
                        ))
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .build();
    }

    @Bean
    ReactiveJwtDecoder reactiveJwtDecoder(JwtProperties jwtProperties) {
        SecretKey secretKey = new SecretKeySpec(
                jwtProperties.secret().getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
        );

        return NimbusReactiveJwtDecoder
                .withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }

    private Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter scopeConverter = new JwtGrantedAuthoritiesConverter();

        JwtGrantedAuthoritiesConverter roleConverter = new JwtGrantedAuthoritiesConverter();
        roleConverter.setAuthoritiesClaimName("roles");
        roleConverter.setAuthorityPrefix("ROLE_");

        return new ReactiveJwtAuthenticationConverterAdapter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            Collection<GrantedAuthority> scopes = scopeConverter.convert(jwt);
            Collection<GrantedAuthority> roles = roleConverter.convert(jwt);

            if (scopes != null) {
                authorities.addAll(scopes);
            }

            if (roles != null) {
                authorities.addAll(roles);
            }

            return new JwtAuthenticationToken(jwt, authorities);
        });
    }

    private Mono<Void> writeErrorResponse(
            ServerWebExchange exchange,
            ObjectMapper objectMapper,
            HttpStatus status,
            String message,
            Exception exception
    ) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", Instant.now());
        body.put("method", exchange.getRequest().getMethod().name());
        body.put("status", status.value());
        body.put("result", status.getReasonPhrase());
        body.put("developerMessage", developerMessage(exception));
        body.put("message", message);
        body.put("path", exchange.getRequest().getURI().getRawPath());
        body.put("data", null);

        try {
            byte[] bytes = objectMapper.writeValueAsBytes(body);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (JsonProcessingException serializationException) {
            return response.setComplete();
        }
    }

    private String developerMessage(Exception exception) {
        if (exception instanceof AuthenticationException || exception instanceof AccessDeniedException) {
            return exception.getClass().getSimpleName();
        }

        return "GatewayException";
    }
}
