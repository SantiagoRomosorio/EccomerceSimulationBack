package com.ecommerce.commerce.adapter.port.out.catalog;

import com.ecommerce.commerce.application.port.out.ProductCatalogPort;
import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import com.ecommerce.commerce.domain.exception.CatalogUnavailableException;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import com.ecommerce.common.web.response.ApiResponse;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogProductClientAdapter implements ProductCatalogPort {

    private final RestClient restClient;

    public CatalogProductClientAdapter(RestClient.Builder restClientBuilder, CatalogClientProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public ProductDetails getProduct(UUID productId) {
        try {
            ApiResponse<CatalogProductResponse> response = restClient.get()
                    .uri("/api/products/{id}", productId)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null || response.data() == null) {
                throw new IllegalStateException("Catalog returned an empty product response");
            }

            CatalogProductResponse product = response.data();
            if (!productId.equals(product.id())) {
                throw new IllegalStateException("Catalog returned a different product");
            }

            return new ProductDetails(
                    product.id(),
                    product.sku(),
                    product.name(),
                    product.price(),
                    product.currency()
            );
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                throw new ResourceNotFoundException("Product not found", Map.of(
                        "productId", productId,
                        "catalogStatus", exception.getStatusCode().value()
                ));
            }

            throw exception;
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CatalogUnavailableException(exception);
        }
    }

    private record CatalogProductResponse(
            UUID id,
            String sku,
            String name,
            BigDecimal price,
            String currency
    ) {
    }
}
