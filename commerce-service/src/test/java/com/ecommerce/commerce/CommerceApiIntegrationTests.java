package com.ecommerce.commerce;

import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class CommerceApiIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductInventoryPort productInventoryPort;

    @Test
    void healthReturnsStandardApiResponse() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.result").value("OK"))
                .andExpect(jsonPath("$.message").value("Service is healthy"))
                .andExpect(jsonPath("$.data.service").value("commerce-service"));
    }

    @Test
    void cartCheckoutAndOrdersUseStandardApiResponses() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        mockMvc.perform(get("/api/cart").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Cart returned successfully"))
                .andExpect(jsonPath("$.data.userId").value(userId.toString()))
                .andExpect(jsonPath("$.data.items.length()").value(0));

        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sku": "SKU-001",
                                  "productName": "Keyboard",
                                  "unitPrice": 25.50,
                                  "currency": "USD",
                                  "quantity": 2
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Cart item added successfully"))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andExpect(jsonPath("$.data.items[0].quantity").value(2))
                .andExpect(jsonPath("$.data.total").value(51.0));

        mockMvc.perform(patch("/api/cart/items/{productId}", productId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "quantity": 3
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item updated successfully"))
                .andExpect(jsonPath("$.data.items[0].quantity").value(3))
                .andExpect(jsonPath("$.data.total").value(76.5));

        MvcResult checkout = mockMvc.perform(post("/api/checkout")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Order created successfully"))
                .andExpect(jsonPath("$.data.id", notNullValue()))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.shippingAddress.recipientName").value("Smoke Buyer"))
                .andExpect(jsonPath("$.data.billingAddress.recipientName").value("Billing Buyer"))
                .andExpect(jsonPath("$.data.notes").value("Deliver during business hours"))
                .andExpect(jsonPath("$.data.items[0].productId").value(productId.toString()))
                .andReturn();

        String orderId = com.jayway.jsonpath.JsonPath.read(
                checkout.getResponse().getContentAsString(),
                "$.data.id"
        );

        mockMvc.perform(get("/api/orders").header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Orders returned successfully"))
                .andExpect(jsonPath("$.data[0].id").value(orderId));

        mockMvc.perform(get("/api/orders/{id}", orderId).header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Order returned successfully"))
                .andExpect(jsonPath("$.data.id").value(orderId))
                .andExpect(jsonPath("$.data.shippingAddress.city").value("Bogota"));

        mockMvc.perform(post("/api/orders/{id}/payment-confirmations", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "providerReference": "pay-test-001"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Order payment confirmed successfully"))
                .andExpect(jsonPath("$.data.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.data.paymentMethod").value("CARD"))
                .andExpect(jsonPath("$.data.paymentReference").value("pay-test-001"))
                .andExpect(jsonPath("$.data.paidAt", notNullValue()));

        mockMvc.perform(post("/api/orders/{id}/payment-confirmations", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "providerReference": "pay-test-002"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.developerMessage").value("InvalidOrderStateException"));
    }

    @Test
    void addItemReturnsBadRequestWhenPayloadIsInvalid() throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", UUID.randomUUID())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": null,
                                  "sku": "",
                                  "productName": "",
                                  "unitPrice": 0,
                                  "currency": "US",
                                  "quantity": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"));
    }

    @Test
    void checkoutReturnsBadRequestWhenAddressIsInvalid() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        addItem(userId, productId);

        mockMvc.perform(post("/api/checkout")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "shippingAddress": {
                                    "recipientName": "",
                                    "line1": "Street 1",
                                    "city": "Bogota",
                                    "region": "Bogota",
                                    "postalCode": "110111",
                                    "country": "COL",
                                    "phone": "3000000000"
                                  }
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.developerMessage").value("Request body validation failed"));
    }

    @Test
    void cancelPendingOrderReturnsOkAndReleasesStock() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        addItem(userId, productId);

        MvcResult checkout = mockMvc.perform(post("/api/checkout")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andReturn();

        String orderId = com.jayway.jsonpath.JsonPath.read(
                checkout.getResponse().getContentAsString(),
                "$.data.id"
        );

        mockMvc.perform(post("/api/orders/{id}/cancellations", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Customer changed their mind"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Order cancelled successfully"))
                .andExpect(jsonPath("$.data.status").value("CANCELLED"))
                .andExpect(jsonPath("$.data.cancellationReason").value("Customer changed their mind"))
                .andExpect(jsonPath("$.data.cancelledAt", notNullValue()));

        verify(productInventoryPort).releaseStock(java.util.List.of(
                new ProductInventoryPort.Reservation(productId, 1)
        ));
    }

    @Test
    void cancelConfirmedOrderReturnsConflict() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        addItem(userId, productId);

        MvcResult checkout = mockMvc.perform(post("/api/checkout")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validCheckoutBody()))
                .andExpect(status().isCreated())
                .andReturn();

        String orderId = com.jayway.jsonpath.JsonPath.read(
                checkout.getResponse().getContentAsString(),
                "$.data.id"
        );

        mockMvc.perform(post("/api/orders/{id}/payment-confirmations", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentMethod": "CARD",
                                  "providerReference": "pay-confirmed-cancel"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/orders/{id}/cancellations", orderId)
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "reason": "Too late"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.method").value("POST"))
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.developerMessage").value("InvalidOrderStateException"));
    }

    @Test
    void endpointsReturnUnauthorizedWhenInternalUserHeaderIsMissing() throws Exception {
        mockMvc.perform(get("/api/cart"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.method").value("GET"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    @Test
    void removeItemReturnsCurrentCart() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        addItem(userId, productId);

        mockMvc.perform(delete("/api/cart/items/{productId}", productId).header("X-User-Id", userId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Cart item removed successfully"))
                .andExpect(jsonPath("$.data.items.length()").value(0));
    }

    private void addItem(UUID userId, UUID productId) throws Exception {
        mockMvc.perform(post("/api/cart/items")
                        .header("X-User-Id", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "%s",
                                  "sku": "SKU-REMOVE",
                                  "productName": "Mouse",
                                  "unitPrice": 10.00,
                                  "currency": "USD",
                                  "quantity": 1
                                }
                                """.formatted(productId)))
                .andExpect(status().isCreated());
    }

    private String validCheckoutBody() {
        return """
                {
                  "shippingAddress": {
                    "recipientName": "Smoke Buyer",
                    "line1": "Street 1 # 2-3",
                    "line2": "Apartment 401",
                    "city": "Bogota",
                    "region": "Bogota",
                    "postalCode": "110111",
                    "country": "CO",
                    "phone": "3000000000"
                  },
                  "billingAddress": {
                    "recipientName": "Billing Buyer",
                    "line1": "Billing Street 4",
                    "city": "Medellin",
                    "region": "Antioquia",
                    "postalCode": "050001",
                    "country": "CO",
                    "phone": "3010000000"
                  },
                  "notes": "Deliver during business hours"
                }
                """;
    }
}
