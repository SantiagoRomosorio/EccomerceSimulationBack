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
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("local")
class ApiGatewayLocalProfileTests {

    @Autowired
    private RouteDefinitionLocator routeDefinitionLocator;

    @Test
    void localProfileRoutesToLocalServicePorts() {
        Map<String, RouteDefinition> routes = routeDefinitionsById();

        assertThat(routes.get("identity-service").getUri()).isEqualTo(URI.create("http://localhost:8081"));
        assertThat(routes.get("identity-health").getUri()).isEqualTo(URI.create("http://localhost:8081"));
        assertThat(routes.get("catalog-service").getUri()).isEqualTo(URI.create("http://localhost:8082"));
        assertThat(routes.get("catalog-health").getUri()).isEqualTo(URI.create("http://localhost:8082"));
        assertThat(routes.get("commerce-service").getUri()).isEqualTo(URI.create("http://localhost:8083"));
        assertThat(routes.get("commerce-health").getUri()).isEqualTo(URI.create("http://localhost:8083"));
    }

    private Map<String, RouteDefinition> routeDefinitionsById() {
        return routeDefinitionLocator.getRouteDefinitions()
                .collectList()
                .blockOptional(Duration.ofSeconds(5))
                .orElseThrow()
                .stream()
                .collect(Collectors.toMap(RouteDefinition::getId, Function.identity()));
    }
}
