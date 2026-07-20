package com.ecommerce.commerce.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.commerce.application.port.in.AddCartItemUseCase;
import com.ecommerce.commerce.application.port.in.CheckoutUseCase;
import com.ecommerce.commerce.application.port.out.CartRepositoryPort;
import com.ecommerce.commerce.application.port.out.OrderRepositoryPort;
import com.ecommerce.commerce.application.port.out.ProductCatalogPort;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.domain.exception.InvalidCartException;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import com.ecommerce.commerce.domain.model.Order;
import com.ecommerce.commerce.domain.model.OrderAddress;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
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
    private OrderRepositoryPort orderRepository;

    @Mock
    private ProductCatalogPort productCatalogPort;

    @Mock
    private ProductInventoryPort productInventoryPort;

    private CommerceService service;

    @BeforeEach
    void setUp() {
        service = new CommerceService(
                cartRepository,
                orderRepository,
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
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));
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
        verify(productInventoryPort).reserveStock(List.of(
                new ProductInventoryPort.Reservation(productId, 2)
        ));
        verify(cartRepository).deleteById(cartId);

        InOrder calls = inOrder(productCatalogPort, productInventoryPort);
        calls.verify(productCatalogPort).getProduct(productId);
        calls.verify(productInventoryPort).reserveStock(any());
    }
}
