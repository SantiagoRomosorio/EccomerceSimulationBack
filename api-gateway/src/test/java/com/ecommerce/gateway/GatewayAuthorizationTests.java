package com.ecommerce.gateway;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.server.RequestPredicates;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "internal.gateway-token=test-internal-gateway-token",
        "spring.cloud.gateway.server.webflux.routes[0].id=authorization-test",
        "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/__gateway-test/ok",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/**"
})
@Import(GatewayAuthorizationTests.TestBackendConfig.class)
class GatewayAuthorizationTests {

    private static final String JWT_SECRET = "change-this-secret-key-change-this-secret-key";
    private static final String OTHER_JWT_SECRET = "other-secret-key-other-secret-key-other-secret";
    private static final String PRODUCT_ID = "11111111-1111-1111-1111-111111111111";
    private static final String ORDER_ID = "22222222-2222-2222-2222-222222222222";

    @Autowired
    private WebTestClient webTestClient;

    @Test
    void protectedRouteReturnsUnauthorizedWithoutJwt() throws JOSEException {
        exchange(HttpMethod.GET, "/api/products", null)
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication required");
    }

    @ParameterizedTest
    @CsvSource({
            "POST,/api/auth/login",
            "POST,/api/auth/register",
            "GET,/api/identity/health",
            "GET,/api/catalog/health",
            "GET,/api/commerce/health",
            "GET,/api/identity/v3/api-docs",
            "GET,/api/catalog/v3/api-docs",
            "GET,/api/commerce/v3/api-docs"
    })
    void publicRoutesAllowRequestsWithoutJwt(HttpMethod method, String path) {
        webTestClient.method(method)
                .uri(path)
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok")
                .jsonPath("$.headers.hasUserId").isEqualTo(false)
                .jsonPath("$.headers.hasUserEmail").isEqualTo(false)
                .jsonPath("$.headers.hasUserRoles").isEqualTo(false)
                .jsonPath("$.headers.hasUserScopes").isEqualTo(false);
    }

    @Test
    void publicRouteStripsClientIdentityHeadersWithoutJwt() {
        webTestClient.post()
                .uri("/api/auth/login")
                .header("X-User-Id", "attacker-user-id")
                .header("X-User-Email", "attacker@example.com")
                .header("X-User-Roles", "ADMIN")
                .header("X-User-Scopes", "stock:manage")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.hasUserId").isEqualTo(false)
                .jsonPath("$.headers.hasUserEmail").isEqualTo(false)
                .jsonPath("$.headers.hasUserRoles").isEqualTo(false)
                .jsonPath("$.headers.hasUserScopes").isEqualTo(false);
    }

    @Test
    void gatewayStripsClientInternalGatewayTokenAndInjectsTrustedValue() {
        webTestClient.post()
                .uri("/api/auth/login")
                .header("X-Internal-Gateway-Token", "attacker-token")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.internalGatewayToken").isEqualTo("test-internal-gateway-token");
    }

    @Test
    void protectedRouteReturnsUnauthorizedWithInvalidJwt() {
        webTestClient.get()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer not-a-jwt")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication required");
    }

    @Test
    void protectedRouteReturnsUnauthorizedWithWrongSignature() throws JOSEException {
        String token = jwt("catalog:read", OTHER_JWT_SECRET, Instant.now().plusSeconds(3600));

        webTestClient.get()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication required");
    }

    @Test
    void protectedRouteReturnsUnauthorizedWithExpiredJwt() throws JOSEException {
        String token = jwt("catalog:read", JWT_SECRET, Instant.now().minusSeconds(60));

        webTestClient.get()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .exchange()
                .expectStatus().isUnauthorized()
                .expectBody()
                .jsonPath("$.status").isEqualTo(401)
                .jsonPath("$.message").isEqualTo("Authentication required");
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/products,catalog:read",
            "GET,/api/products/" + PRODUCT_ID + ",catalog:read",
            "GET,/api/categories,catalog:read",
            "GET,/api/brands,catalog:read",
            "POST,/api/products,catalog:write",
            "POST,/api/categories,catalog:write",
            "POST,/api/brands,catalog:write",
            "PATCH,/api/products/" + PRODUCT_ID + "/stock,stock:manage",
            "GET,/api/cart,cart:manage",
            "POST,/api/cart/items,cart:manage",
            "PATCH,/api/cart/items/" + PRODUCT_ID + ",cart:manage",
            "DELETE,/api/cart/items/" + PRODUCT_ID + ",cart:manage",
            "POST,/api/checkout,checkout:create",
            "GET,/api/orders,orders:read:self",
            "GET,/api/orders/" + ORDER_ID + ",orders:read:self",
            "POST,/api/orders/" + ORDER_ID + "/payment-confirmations,orders:pay:self",
            "POST,/api/orders/" + ORDER_ID + "/cancellations,orders:cancel:self",
            "GET,/api/users/me,users:read:self"
    })
    void protectedRoutesAllowMatchingScope(HttpMethod method, String path, String scope) throws JOSEException {
        exchange(method, path, scope)
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.status").isEqualTo("ok");
    }

    @ParameterizedTest
    @CsvSource({
            "GET,/api/products,cart:manage",
            "POST,/api/products,catalog:read",
            "PATCH,/api/products/" + PRODUCT_ID + "/stock,catalog:write",
            "GET,/api/cart,catalog:read",
            "POST,/api/checkout,cart:manage",
            "GET,/api/orders,cart:manage",
            "POST,/api/orders/" + ORDER_ID + "/payment-confirmations,orders:read:self",
            "POST,/api/orders/" + ORDER_ID + "/cancellations,orders:read:self",
            "GET,/api/users/me,catalog:read"
    })
    void protectedRoutesRejectWrongScope(HttpMethod method, String path, String scope) throws JOSEException {
        exchange(method, path, scope)
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.status").isEqualTo(403)
                .jsonPath("$.message").isEqualTo("Forbidden");
    }

    @Test
    void gatewayStripsClientIdentityHeadersAndInjectsTrustedJwtClaims() throws JOSEException {
        String userId = UUID.randomUUID().toString();

        webTestClient.get()
                .uri("/api/products")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtWithClaims(
                        userId,
                        "customer@example.com",
                        List.of("CUSTOMER"),
                        "catalog:read"
                ))
                .header("X-User-Id", "attacker-user-id")
                .header("X-User-Email", "attacker@example.com")
                .header("X-User-Roles", "ADMIN")
                .header("X-User-Scopes", "stock:manage")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.headers.userId").isEqualTo(userId)
                .jsonPath("$.headers.userEmail").isEqualTo("customer@example.com")
                .jsonPath("$.headers.userRoles").isEqualTo("CUSTOMER")
                .jsonPath("$.headers.userScopes").isEqualTo("catalog:read");
    }

    private WebTestClient.ResponseSpec exchange(HttpMethod method, String path, String scope) throws JOSEException {
        WebTestClient.RequestBodySpec request = webTestClient.method(method).uri(path);

        if (scope != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtWithScope(scope));
        }

        return request.exchange();
    }

    private String jwtWithScope(String scope) throws JOSEException {
        return jwtWithClaims(
                UUID.randomUUID().toString(),
                "customer@example.com",
                List.of("CUSTOMER"),
                scope
        );
    }

    private String jwtWithClaims(
            String subject,
            String email,
            List<String> roles,
            String scope
    ) throws JOSEException {
        return jwt(subject, email, roles, scope, JWT_SECRET, Instant.now().plusSeconds(3600));
    }

    private String jwt(String scope, String secret, Instant expiresAt) throws JOSEException {
        return jwt(UUID.randomUUID().toString(), "customer@example.com", List.of("CUSTOMER"), scope, secret, expiresAt);
    }

    private String jwt(
            String subject,
            String email,
            List<String> roles,
            String scope,
            String secret,
            Instant expiresAt
    ) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(subject)
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiresAt))
                .claim("email", email)
                .claim("roles", roles)
                .claim("scope", scope)
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(secret.getBytes(StandardCharsets.UTF_8)));

        return jwt.serialize();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoute() {
            return RouterFunctions.route(
                    RequestPredicates.path("/__gateway-test/ok"),
                    request -> ServerResponse.ok().bodyValue(Map.of(
                            "status", "ok",
                            "headers", identityHeaders(request)
                    ))
            );
        }

        private Map<String, Object> identityHeaders(ServerRequest request) {
            Map<String, Object> headers = new LinkedHashMap<>();
            String userId = request.headers().firstHeader("X-User-Id");
            String userEmail = request.headers().firstHeader("X-User-Email");
            String userRoles = request.headers().firstHeader("X-User-Roles");
            String userScopes = request.headers().firstHeader("X-User-Scopes");
            String internalGatewayToken = request.headers().firstHeader("X-Internal-Gateway-Token");

            headers.put("hasUserId", userId != null);
            headers.put("hasUserEmail", userEmail != null);
            headers.put("hasUserRoles", userRoles != null);
            headers.put("hasUserScopes", userScopes != null);
            headers.put("userId", userId);
            headers.put("userEmail", userEmail);
            headers.put("userRoles", userRoles);
            headers.put("userScopes", userScopes);
            headers.put("hasInternalGatewayToken", internalGatewayToken != null);
            headers.put("internalGatewayToken", internalGatewayToken);

            return headers;
        }
    }
}
