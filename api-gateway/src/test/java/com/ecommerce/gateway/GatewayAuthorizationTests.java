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
import org.springframework.web.reactive.function.server.ServerResponse;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.gateway.server.webflux.routes[0].id=authorization-test",
        "spring.cloud.gateway.server.webflux.routes[0].uri=forward:/__gateway-test/ok",
        "spring.cloud.gateway.server.webflux.routes[0].predicates[0]=Path=/api/**"
})
@Import(GatewayAuthorizationTests.TestBackendConfig.class)
class GatewayAuthorizationTests {

    private static final String JWT_SECRET = "change-this-secret-key-change-this-secret-key";
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

    private WebTestClient.ResponseSpec exchange(HttpMethod method, String path, String scope) throws JOSEException {
        WebTestClient.RequestBodySpec request = webTestClient.method(method).uri(path);

        if (scope != null) {
            request.header(HttpHeaders.AUTHORIZATION, "Bearer " + jwtWithScope(scope));
        }

        return request.exchange();
    }

    private String jwtWithScope(String scope) throws JOSEException {
        Instant now = Instant.now();
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .subject(UUID.randomUUID().toString())
                .issueTime(Date.from(now))
                .expirationTime(Date.from(now.plusSeconds(3600)))
                .claim("email", "customer@example.com")
                .claim("roles", List.of("CUSTOMER"))
                .claim("scope", scope)
                .build();

        SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
        jwt.sign(new MACSigner(JWT_SECRET.getBytes(StandardCharsets.UTF_8)));

        return jwt.serialize();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class TestBackendConfig {

        @Bean
        RouterFunction<ServerResponse> testBackendRoute() {
            return RouterFunctions.route(
                    RequestPredicates.path("/__gateway-test/ok"),
                    request -> ServerResponse.ok().bodyValue(Map.of("status", "ok"))
            );
        }
    }
}
