package com.ecommerce.commerce.adapter.port.out.catalog;

import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import com.ecommerce.commerce.domain.exception.InventoryReservationException;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

@Component
public class CatalogInventoryClientAdapter implements ProductInventoryPort {

    private final RestClient restClient;

    public CatalogInventoryClientAdapter(RestClient.Builder restClientBuilder, CatalogClientProperties properties) {
        this.restClient = restClientBuilder
                .baseUrl(properties.baseUrl())
                .build();
    }

    @Override
    public void reserveStock(List<Reservation> reservations) {
        try {
            restClient.post()
                    .uri("/api/internal/products/stock/reservations")
                    .body(new ReserveStockRequest(reservations.stream()
                            .map(item -> new ReserveStockItemRequest(item.productId(), item.quantity()))
                            .toList()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                throw new ResourceNotFoundException("Product not found while reserving stock", Map.of(
                        "catalogStatus", exception.getStatusCode().value()
                ));
            }

            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(409))) {
                throw new InventoryReservationException("Insufficient product stock", Map.of(
                        "catalogStatus", exception.getStatusCode().value()
                ));
            }

            throw new InventoryReservationException("Catalog rejected stock reservation", Map.of(
                    "catalogStatus", exception.getStatusCode().value()
            ));
        }
    }

    @Override
    public void releaseStock(List<Reservation> reservations) {
        try {
            restClient.post()
                    .uri("/api/internal/products/stock/releases")
                    .body(new ReserveStockRequest(reservations.stream()
                            .map(item -> new ReserveStockItemRequest(item.productId(), item.quantity()))
                            .toList()))
                    .retrieve()
                    .toBodilessEntity();
        } catch (HttpClientErrorException exception) {
            if (exception.getStatusCode().isSameCodeAs(HttpStatusCode.valueOf(404))) {
                throw new ResourceNotFoundException("Product not found while releasing stock", Map.of(
                        "catalogStatus", exception.getStatusCode().value()
                ));
            }

            throw new InventoryReservationException("Catalog rejected stock release", Map.of(
                    "catalogStatus", exception.getStatusCode().value()
            ));
        }
    }

    private record ReserveStockRequest(List<ReserveStockItemRequest> items) {
    }

    private record ReserveStockItemRequest(UUID productId, int quantity) {
    }
}
