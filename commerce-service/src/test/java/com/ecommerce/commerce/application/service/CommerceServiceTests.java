package com.ecommerce.commerce.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.commerce.application.port.in.AddCartItemUseCase;
import com.ecommerce.commerce.application.port.in.CancelOrderUseCase;
import com.ecommerce.commerce.application.port.in.CheckoutUseCase;
import com.ecommerce.commerce.application.port.in.ConfirmOrderPaymentUseCase;
import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.CheckoutPersistencePort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderTransitionPort;
import com.ecommerce.commerce.application.port.out.ProductCatalogPort;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.domain.exception.InvalidCartException;
import com.ecommerce.commerce.domain.exception.InvalidOrderStateException;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderAddress;
import com.ecommerce.commerce.domain.model.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.mockito.ArgumentCaptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommerceServiceTests {

    @Mock
    private CartRepositoryPort cartRepository;

    @Mock
    private CheckoutPersistencePort checkoutPersistencePort;

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private OrderTransitionPort orderTransitionPort;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private ProductInventoryPort productInventoryPort;

    private CommerceService service;

    @BeforeEach
    void setUp() {
        service = new CommerceService(
                cartRepository,
                checkoutPersistencePort,
                orderRepository,
                orderTransitionPort,
                productCatalogPort,
                productInventoryPort
        );
    }

    @Test
    void addsCartItemWithCanonicalProductDetails() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(productCatalogPort.getProduct(productId)).thenReturn(new ProductCatalogPort.ProductDetails(
                productId,
                "CATALOG-SKU",
                "Catalog Product",
                new BigDecimal("49.99"),
                "USD"
        ));
        when(cartRepository.save(any(Cart.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cart cart = service.addItem(userId, new AddCartItemUseCase.Command(productId, 2));

        assertThat(cart.items()).singleElement().satisfies(item -> {
            assertThat(item.productId()).isEqualTo(productId);
            assertThat(item.sku()).isEqualTo("CATALOG-SKU");
            assertThat(item.productName()).isEqualTo("Catalog Product");
            assertThat(item.unitPrice()).isEqualByComparingTo("49.99");
            assertThat(item.currency()).isEqualTo("USD");
            assertThat(item.quantity()).isEqualTo(2);
        });
        assertThat(cart.total()).isEqualByComparingTo("99.98");
        verify(productCatalogPort).getProduct(productId);
    }

    @Test
    void rejectsMergedCartItemQuantityAboveMaximum() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = new Cart(UUID.randomUUID(), userId, List.of(new CartItem(
                UUID.randomUUID(),
                productId,
                "SKU-001",
                "Product",
                BigDecimal.ONE,
                "USD",
                1_000
        )));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.getProduct(productId)).thenReturn(new ProductCatalogPort.ProductDetails(
                productId,
                "SKU-001",
                "Product",
                BigDecimal.ONE,
                "USD"
        ));

        assertThatThrownBy(() -> service.addItem(userId, new AddCartItemUseCase.Command(productId, 1)))
                .isInstanceOf(InvalidCartException.class)
                .hasMessage("Cart item quantity exceeds maximum");

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void convertsCartItemQuantityOverflowIntoControlledDomainError() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = new Cart(UUID.randomUUID(), userId, List.of(new CartItem(
                UUID.randomUUID(),
                productId,
                "SKU-001",
                "Product",
                BigDecimal.ONE,
                "USD",
                Integer.MAX_VALUE
        )));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.getProduct(productId)).thenReturn(new ProductCatalogPort.ProductDetails(
                productId,
                "SKU-001",
                "Product",
                BigDecimal.ONE,
                "USD"
        ));

        assertThatThrownBy(() -> service.addItem(userId, new AddCartItemUseCase.Command(productId, 1)))
                .isInstanceOf(InvalidCartException.class)
                .hasMessage("Cart item quantity exceeds maximum");

        verify(cartRepository, never()).save(any(Cart.class));
    }

    @Test
    void refreshesProductDetailsBeforeStockReservationAndCheckout() {
        UUID userId = UUID.randomUUID();
        UUID cartId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = new Cart(cartId, userId, List.of(new CartItem(
                UUID.randomUUID(),
                productId,
                "STALE-SKU",
                "Stale Product",
                BigDecimal.ONE,
                "COP",
                2
        )));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.getProduct(productId)).thenReturn(new ProductCatalogPort.ProductDetails(
                productId,
                "CURRENT-SKU",
                "Current Product",
                new BigDecimal("30.00"),
                "USD"
        ));
        when(checkoutPersistencePort.saveOrderAndDeleteCart(any(Order.class), any(Cart.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        OrderAddress address = new OrderAddress(
                "Buyer",
                "123 Main Street",
                null,
                "Bogota",
                "Cundinamarca",
                "110111",
                "CO",
                "+5700000000"
        );

        Order order = service.checkout(userId, new CheckoutUseCase.Command(address, null, null));

        assertThat(order.currency()).isEqualTo("USD");
        assertThat(order.total()).isEqualByComparingTo("60.00");
        assertThat(order.items()).singleElement().satisfies(item -> {
            assertThat(item.sku()).isEqualTo("CURRENT-SKU");
            assertThat(item.productName()).isEqualTo("Current Product");
            assertThat(item.unitPrice()).isEqualByComparingTo("30.00");
            assertThat(item.lineTotal()).isEqualByComparingTo("60.00");
        });
        verify(productInventoryPort).reserveStock(order.id(), List.of(
                new ProductInventoryPort.Reservation(productId, 2)
        ));
        verify(checkoutPersistencePort).saveOrderAndDeleteCart(any(Order.class), any(Cart.class));

        InOrder calls = inOrder(productCatalogPort, productInventoryPort);
        calls.verify(productCatalogPort).getProduct(productId);
        calls.verify(productInventoryPort).reserveStock(any(), any());
    }

    @Test
    void releasesReservedStockWhenCheckoutPersistenceFails() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        Cart cart = new Cart(UUID.randomUUID(), userId, List.of(new CartItem(
                UUID.randomUUID(),
                productId,
                "SKU-001",
                "Product",
                BigDecimal.TEN,
                "USD",
                2
        )));
        when(cartRepository.findByUserId(userId)).thenReturn(Optional.of(cart));
        when(productCatalogPort.getProduct(productId)).thenReturn(new ProductCatalogPort.ProductDetails(
                productId,
                "SKU-001",
                "Product",
                BigDecimal.TEN,
                "USD"
        ));
        when(checkoutPersistencePort.saveOrderAndDeleteCart(any(Order.class), any(Cart.class)))
                .thenThrow(new IllegalStateException("Database write failed"));

        OrderAddress address = new OrderAddress(
                "Buyer",
                "123 Main Street",
                null,
                "Bogota",
                "Cundinamarca",
                "110111",
                "CO",
                "+5700000000"
        );

        assertThatThrownBy(() -> service.checkout(
                userId,
                new CheckoutUseCase.Command(address, null, null)
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Database write failed");

        ArgumentCaptor<UUID> reservationId = ArgumentCaptor.forClass(UUID.class);
        verify(productInventoryPort).reserveStock(reservationId.capture(), any());
        verify(productInventoryPort).releaseStock(reservationId.getValue());
    }

    @Test
    void confirmsPaymentWithConditionalTransition() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order pendingOrder = order(
                orderId,
                userId,
                OrderStatus.PENDING_PAYMENT,
                null,
                null,
                null
        );
        Order confirmedOrder = order(
                orderId,
                userId,
                OrderStatus.CONFIRMED,
                "CARD",
                "payment-001",
                null
        );
        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(pendingOrder), Optional.of(confirmedOrder));
        when(orderTransitionPort.confirmPayment(any(), any(), any(), any(), any())).thenReturn(true);

        Order result = service.confirmPayment(
                userId,
                orderId,
                new ConfirmOrderPaymentUseCase.Command("CARD", "payment-001")
        );

        assertThat(result).isEqualTo(confirmedOrder);
        verify(orderTransitionPort).confirmPayment(
                any(UUID.class),
                any(UUID.class),
                any(String.class),
                any(String.class),
                any(Instant.class)
        );
    }

    @Test
    void repeatedPaymentWithSameMethodAndReferenceIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order confirmedOrder = order(
                orderId,
                userId,
                OrderStatus.CONFIRMED,
                "CARD",
                "payment-001",
                null
        );
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(confirmedOrder));

        Order result = service.confirmPayment(
                userId,
                orderId,
                new ConfirmOrderPaymentUseCase.Command("CARD", "payment-001")
        );

        assertThat(result).isEqualTo(confirmedOrder);
        verify(orderTransitionPort, never()).confirmPayment(any(), any(), any(), any(), any());
    }

    @Test
    void concurrentIdenticalPaymentReturnsTheWinningConfirmation() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order pendingOrder = order(orderId, userId, OrderStatus.PENDING_PAYMENT, null, null, null);
        Order confirmedOrder = order(
                orderId,
                userId,
                OrderStatus.CONFIRMED,
                "CARD",
                "payment-001",
                null
        );
        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(pendingOrder), Optional.of(confirmedOrder));
        when(orderTransitionPort.confirmPayment(any(), any(), any(), any(), any())).thenReturn(false);

        Order result = service.confirmPayment(
                userId,
                orderId,
                new ConfirmOrderPaymentUseCase.Command("CARD", "payment-001")
        );

        assertThat(result).isEqualTo(confirmedOrder);
    }

    @Test
    void repeatedPaymentWithDifferentReferenceIsRejected() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order confirmedOrder = order(
                orderId,
                userId,
                OrderStatus.CONFIRMED,
                "CARD",
                "payment-001",
                null
        );
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(confirmedOrder));

        assertThatThrownBy(() -> service.confirmPayment(
                userId,
                orderId,
                new ConfirmOrderPaymentUseCase.Command("CARD", "payment-002")
        ))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be paid in current state");

        verify(orderTransitionPort, never()).confirmPayment(any(), any(), any(), any(), any());
    }

    @Test
    void paymentDuringCancellationIsRejected() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order cancellationPendingOrder = order(
                orderId,
                userId,
                OrderStatus.CANCELLATION_PENDING,
                null,
                null,
                "Customer request"
        );
        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(cancellationPendingOrder));

        assertThatThrownBy(() -> service.confirmPayment(
                userId,
                orderId,
                new ConfirmOrderPaymentUseCase.Command("CARD", "payment-001")
        ))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be paid in current state");

        verify(orderTransitionPort, never()).confirmPayment(any(), any(), any(), any(), any());
    }

    @Test
    void claimsCancellationBeforeReleasingStockAndThenCompletesIt() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String reason = "Customer request";
        Order pendingOrder = order(orderId, userId, OrderStatus.PENDING_PAYMENT, null, null, null);
        Order cancellationPendingOrder = order(
                orderId,
                userId,
                OrderStatus.CANCELLATION_PENDING,
                null,
                null,
                reason
        );
        Order cancelledOrder = order(orderId, userId, OrderStatus.CANCELLED, null, null, reason);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(
                Optional.of(pendingOrder),
                Optional.of(cancellationPendingOrder),
                Optional.of(cancelledOrder)
        );
        when(orderTransitionPort.beginCancellation(orderId, userId, reason)).thenReturn(true);
        when(orderTransitionPort.completeCancellation(any(), any(), any())).thenReturn(true);

        Order result = service.cancelOrder(
                userId,
                orderId,
                new CancelOrderUseCase.Command(reason)
        );

        assertThat(result).isEqualTo(cancelledOrder);
        InOrder calls = inOrder(orderTransitionPort, productInventoryPort);
        calls.verify(orderTransitionPort).beginCancellation(orderId, userId, reason);
        calls.verify(productInventoryPort).releaseStock(orderId);
        calls.verify(orderTransitionPort).completeCancellation(
                any(UUID.class),
                any(UUID.class),
                any(Instant.class)
        );
    }

    @Test
    void cancellationLosingToConcurrentPaymentDoesNotReleaseStock() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String reason = "Customer request";
        Order pendingOrder = order(orderId, userId, OrderStatus.PENDING_PAYMENT, null, null, null);
        Order confirmedOrder = order(
                orderId,
                userId,
                OrderStatus.CONFIRMED,
                "CARD",
                "payment-001",
                null
        );
        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(pendingOrder), Optional.of(confirmedOrder));
        when(orderTransitionPort.beginCancellation(orderId, userId, reason)).thenReturn(false);

        assertThatThrownBy(() -> service.cancelOrder(
                userId,
                orderId,
                new CancelOrderUseCase.Command(reason)
        ))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be cancelled in current state");

        verify(productInventoryPort, never()).releaseStock(any());
        verify(orderTransitionPort, never()).completeCancellation(any(), any(), any());
    }

    @Test
    void retriesStockReleaseWhenCancellationRemainsPending() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String reason = "Customer request";
        Order cancellationPendingOrder = order(
                orderId,
                userId,
                OrderStatus.CANCELLATION_PENDING,
                null,
                null,
                reason
        );
        Order cancelledOrder = order(orderId, userId, OrderStatus.CANCELLED, null, null, reason);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(
                Optional.of(cancellationPendingOrder),
                Optional.of(cancellationPendingOrder),
                Optional.of(cancelledOrder)
        );
        doThrow(new IllegalStateException("Catalog unavailable"))
                .doNothing()
                .when(productInventoryPort)
                .releaseStock(orderId);
        when(orderTransitionPort.completeCancellation(any(), any(), any())).thenReturn(true);
        CancelOrderUseCase.Command command = new CancelOrderUseCase.Command(reason);

        assertThatThrownBy(() -> service.cancelOrder(userId, orderId, command))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Catalog unavailable");

        Order result = service.cancelOrder(userId, orderId, command);

        assertThat(result).isEqualTo(cancelledOrder);
        verify(productInventoryPort, times(2)).releaseStock(orderId);
        verify(orderTransitionPort).completeCancellation(any(), any(), any());
        verify(orderTransitionPort, never()).beginCancellation(any(), any(), any());
    }

    @Test
    void repeatedCancellationWithSameReasonIsIdempotent() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        String reason = "Customer request";
        Order cancelledOrder = order(orderId, userId, OrderStatus.CANCELLED, null, null, reason);
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(cancelledOrder));

        Order result = service.cancelOrder(
                userId,
                orderId,
                new CancelOrderUseCase.Command(reason)
        );

        assertThat(result).isEqualTo(cancelledOrder);
        verify(productInventoryPort, never()).releaseStock(any());
        verify(orderTransitionPort, never()).beginCancellation(any(), any(), any());
        verify(orderTransitionPort, never()).completeCancellation(any(), any(), any());
    }

    @Test
    void repeatedCancellationWithDifferentReasonIsRejected() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order cancelledOrder = order(
                orderId,
                userId,
                OrderStatus.CANCELLED,
                null,
                null,
                "Original reason"
        );
        when(orderRepository.findByIdAndUserId(orderId, userId)).thenReturn(Optional.of(cancelledOrder));

        assertThatThrownBy(() -> service.cancelOrder(
                userId,
                orderId,
                new CancelOrderUseCase.Command("Different reason")
        ))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be cancelled in current state");

        verify(productInventoryPort, never()).releaseStock(any());
    }

    @Test
    void cancellationPendingWithDifferentReasonIsRejected() {
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Order cancellationPendingOrder = order(
                orderId,
                userId,
                OrderStatus.CANCELLATION_PENDING,
                null,
                null,
                "Original reason"
        );
        when(orderRepository.findByIdAndUserId(orderId, userId))
                .thenReturn(Optional.of(cancellationPendingOrder));

        assertThatThrownBy(() -> service.cancelOrder(
                userId,
                orderId,
                new CancelOrderUseCase.Command("Different reason")
        ))
                .isInstanceOf(InvalidOrderStateException.class)
                .hasMessage("Order cannot be cancelled in current state");

        verify(productInventoryPort, never()).releaseStock(any());
        verify(orderTransitionPort, never()).completeCancellation(any(), any(), any());
    }

    private Order order(
            UUID orderId,
            UUID userId,
            OrderStatus status,
            String paymentMethod,
            String paymentReference,
            String cancellationReason
    ) {
        OrderAddress address = new OrderAddress(
                "Buyer",
                "123 Main Street",
                null,
                "Bogota",
                "Cundinamarca",
                "110111",
                "CO",
                "+5700000000"
        );
        return new Order(
                orderId,
                userId,
                status,
                "USD",
                BigDecimal.TEN,
                address,
                address,
                null,
                paymentMethod,
                paymentReference,
                paymentMethod == null ? null : Instant.parse("2026-01-01T00:00:00Z"),
                cancellationReason,
                status == OrderStatus.CANCELLED ? Instant.parse("2026-01-02T00:00:00Z") : null,
                Instant.parse("2026-01-01T00:00:00Z"),
                List.of()
        );
    }
}
