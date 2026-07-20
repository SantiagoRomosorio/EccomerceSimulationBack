package com.ecommerce.commerce.adapter.port.out.catalog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ecommerce.commerce.application.port.out.ProductCatalogPort;
import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import com.ecommerce.commerce.domain.exception.CatalogUnavailableException;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

class CatalogProductClientAdapterTests {

    @Test
    void getsCanonicalProductDetailsFromCatalogApiResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogProductClientAdapter adapter = new CatalogProductClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID productId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/products/" + productId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        {
                          "timestamp": "2026-07-20T12:00:00Z",
                          "method": "GET",
                          "status": 200,
                          "result": "OK",
                          "developerMessage": null,
                          "message": "Product returned successfully",
                          "path": "/api/products/%s",
                          "data": {
                            "id": "%s",
                            "sku": "CATALOG-SKU",
                            "name": "Catalog Product",
                            "description": "Canonical product",
                            "price": 79.90,
                            "currency": "USD",
                            "stockQuantity": 8,
                            "active": true
                          }
                        }
                        """.formatted(productId, productId), MediaType.APPLICATION_JSON));

        ProductCatalogPort.ProductDetails product = adapter.getProduct(productId);

        assertThat(product.id()).isEqualTo(productId);
        assertThat(product.sku()).isEqualTo("CATALOG-SKU");
        assertThat(product.name()).isEqualTo("Catalog Product");
        assertThat(product.price()).isEqualByComparingTo(new BigDecimal("79.90"));
        assertThat(product.currency()).isEqualTo("USD");
        server.verify();
    }

    @Test
    void mapsCatalogNotFoundResponseToDomainException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogProductClientAdapter adapter = new CatalogProductClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID productId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/products/" + productId))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> adapter.getProduct(productId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Product not found");
        server.verify();
    }

    @Test
    void mapsCatalogServerErrorToUnavailableWithoutExposingItsResponse() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogProductClientAdapter adapter = new CatalogProductClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID productId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/products/" + productId))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("sensitive catalog error"));

        assertThatThrownBy(() -> adapter.getProduct(productId))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessage("Catalog service is temporarily unavailable")
                .extracting("details")
                .isEqualTo(java.util.Map.of("service", "catalog"));
        server.verify();
    }

    @Test
    void mapsCatalogConnectionFailureToUnavailable() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogProductClientAdapter adapter = new CatalogProductClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID productId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/products/" + productId))
                .andRespond(request -> {
                    throw new ResourceAccessException("sensitive network details");
                });

        assertThatThrownBy(() -> adapter.getProduct(productId))
                .isInstanceOf(CatalogUnavailableException.class)
                .hasMessage("Catalog service is temporarily unavailable");
        server.verify();
    }
}
