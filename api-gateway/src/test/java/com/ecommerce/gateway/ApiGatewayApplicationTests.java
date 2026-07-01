package com.ecommerce.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.gateway.route.RouteDefinition;
import org.springframework.cloud.gateway.route.RouteDefinitionLocator;

@SpringBootTest
class ApiGatewayApplicationTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void contextLoads() {
    }

    @Test
    void loadsRoutesForAllCurrentMicroservices() {
        Map<String, RouteDefinition> routes = routeDefinitionsById();

        assertThat(routes).containsOnlyKeys(
                "identity-service",
                "identity-health",
                "catalog-service",
                "catalog-health",
                "commerce-service",
                "commerce-health"
        );

        assertRoute(routes.get("identity-service"), "http://identity-service:8080", "/api/auth/**", "/api/users/**");
        assertRoute(routes.get("catalog-service"), "http://catalog-service:8080", "/api/products/**", "/api/categories/**", "/api/brands/**");
        assertRoute(routes.get("commerce-service"), "http://commerce-service:8080", "/api/cart/**", "/api/checkout/**", "/api/orders/**");
        assertRoute(routes.get("identity-health"), "http://identity-service:8080", "/api/identity/health");
        assertRoute(routes.get("catalog-health"), "http://catalog-service:8080", "/api/catalog/health");
        assertRoute(routes.get("commerce-health"), "http://commerce-service:8080", "/api/commerce/health");

        assertThat(routes.get("identity-health").getFilters())
                .anySatisfy(filter -> assertThat(filter.getArgs())
                        .containsValues("/api/identity/health", "/api/health"));

        assertThat(routes.get("catalog-health").getFilters())
                .anySatisfy(filter -> assertThat(filter.getArgs())
                        .containsValues("/api/catalog/health", "/api/health"));

        assertThat(routes.get("commerce-health").getFilters())
                .anySatisfy(filter -> assertThat(filter.getArgs())
                        .containsValues("/api/commerce/health", "/api/health"));
    }

    private Map<String, RouteDefinition> routeDefinitionsById() {
        return routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .blockOptional(Duration.ofSeconds(5))
                .orElseThrow()
                .stream()
                .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));
    }

    private void assertRoute(RouteDefinition route, String uri, String... pathPatterns) {
        assertThat(route.getUri()).isEqualTo(URI.create(uri));
        assertThat(route.getPredicates())
                .anySatisfy(predicate -> assertThat(predicate.getArgs())
                        .containsValues(pathPatterns));
    }
}
