package com.ecommerce.commerce.adapter.port.out.jpa;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.mapper.CommerceJpaMapper;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.CartJpaRepository;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.OrderJpaRepository;
import com.ecommerce.commerce.domain.exception.ResourceNotFoundException;
import com.ecommerce.commerce.domain.model.Cart;
import com.ecommerce.commerce.domain.model.CartItem;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CommercePersistenceAdapterTests {

    @Mock
    private CartJpaRepository cartRepository;

    @Mock
    private OrderJpaRepository orderRepository;

    private CommercePersistenceAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new CommercePersistenceAdapter(
                cartRepository,
                orderRepository,
                new CommerceJpaMapper()
        );
    }

    @Test
    void locksExistingCartBeforeSavingWriterMutation() {
        UUID cartId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Cart updatedCart = cart(cartId, userId);
        CartEntity persistedCart = new CartEntity();
        persistedCart.setId(cartId);
        persistedCart.setUserId(userId);

        when(cartRepository.findByIdAndUserIdForUpdate(cartId, userId))
                .thenReturn(Optional.of(persistedCart));
        when(cartRepository.save(persistedCart)).thenReturn(persistedCart);

        Cart result = adapter.save(updatedCart);

        assertThat(result).isEqualTo(updatedCart);
        verify(cartRepository).findByIdAndUserIdForUpdate(cartId, userId);
        verify(cartRepository).save(persistedCart);
    }

    @Test
    void doesNotRecreateCartRemovedByCheckoutWhileWriterWasInFlight() {
        UUID cartId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        Cart staleWriterCart = cart(cartId, userId);

        when(cartRepository.findByIdAndUserIdForUpdate(cartId, userId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> adapter.save(staleWriterCart))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Cart not found");

        verify(cartRepository).findByIdAndUserIdForUpdate(cartId, userId);
        verify(cartRepository, never()).save(any(CartEntity.class));
    }

    private Cart cart(UUID cartId, UUID userId) {
        return new Cart(cartId, userId, List.of(new CartItem(
                UUID.randomUUID(),
                UUID.randomUUID(),
                "SKU-001",
                "Product",
                new BigDecimal("10.00"),
                "USD",
                2
        )));
    }
}
