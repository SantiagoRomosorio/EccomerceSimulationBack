package com.ecommerce.commerce.adapter.port.out.catalog;

import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import com.ecommerce.commerce.domain.exception.CatalogUnavailableException;
import com.ecommerce.commerce.domain.exception.InventoryReservationException;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
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
    public void reserveStock(UUID reservationId, List<Reservation> reservations) {
        try {
            restClient.post()
                    .uri("/api/internal/products/stock/reservations")
                    .body(new ReserveStockRequest(reservationId, reservations.stream()
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
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CatalogUnavailableException(exception);
        }
    }

    @Override
    public void releaseStock(UUID reservationId) {
        try {
            restClient.post()
                    .uri("/api/internal/products/stock/releases")
                    .body(new ReleaseStockRequest(reservationId))
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
        } catch (HttpServerErrorException | ResourceAccessException exception) {
            throw new CatalogUnavailableException(exception);
        }
    }

    private record ReserveStockRequest(UUID reservationId, List<ReserveStockItemRequest> items) {
    }

    private record ReserveStockItemRequest(UUID productId, int quantity) {
    }

    private record ReleaseStockRequest(UUID reservationId) {
    }
}
