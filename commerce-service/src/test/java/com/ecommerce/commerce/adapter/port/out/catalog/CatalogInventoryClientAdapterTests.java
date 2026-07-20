package com.ecommerce.commerce.adapter.port.out.catalog;

import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.config.properties.CatalogClientProperties;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class CatalogInventoryClientAdapterTests {

    @Test
    void reservesStockWithStableReservationIdAndItems() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogInventoryClientAdapter adapter = new CatalogInventoryClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID reservationId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/internal/products/stock/reservations"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "reservationId": "%s",
                          "items": [
                            {
                              "productId": "%s",
                              "quantity": 2
                            }
                          ]
                        }
                        """.formatted(reservationId, productId)))
                .andRespond(withSuccess());

        adapter.reserveStock(reservationId, List.of(
                new ProductInventoryPort.Reservation(productId, 2)
        ));

        server.verify();
    }

    @Test
    void releasesStockUsingOnlyTheReservationId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        CatalogInventoryClientAdapter adapter = new CatalogInventoryClientAdapter(
                builder,
                new CatalogClientProperties("http://catalog.test")
        );
        UUID reservationId = UUID.randomUUID();

        server.expect(once(), requestTo("http://catalog.test/api/internal/products/stock/releases"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(content().json("""
                        {
                          "reservationId": "%s"
                        }
                        """.formatted(reservationId)))
                .andRespond(withSuccess());

        adapter.releaseStock(reservationId);

        server.verify();
    }
}
