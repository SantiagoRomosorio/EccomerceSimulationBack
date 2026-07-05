package com.ecommerce.commerce;

import static org.assertj.core.api.Assertions.assertThat;

import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.entity.CartItemEntity;
import com.ecommerce.commerce.adapter.port.out.jpa.repository.CartJpaRepository;
import com.ecommerce.commerce.application.port.in.AddCartItemUseCase;
import com.ecommerce.commerce.application.port.in.GetCartUseCase;
import com.ecommerce.commerce.application.port.in.RemoveCartItemUseCase;
import com.ecommerce.commerce.application.port.out.ProductInventoryPort;
import com.ecommerce.commerce.domain.model.Cart;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

@SpringBootTest
class CartSoftDeleteIntegrationTests {

    @Autowired
    private AddCartItemUseCase addCartItemUseCase;

    @Autowired
    private RemoveCartItemUseCase removeCartItemUseCase;

    @Autowired
    private GetCartUseCase getCartUseCase;

    @Autowired
    private CartJpaRepository cartRepository;

    @MockitoBean
    private ProductInventoryPort productInventoryPort;

    @Test
    void removeItemMarksCartItemAsDeletedAndHidesItFromCurrentCart() {
        UUID userId = UUID.randomUUID();
        UUID productId = UUID.randomUUID();

        addCartItemUseCase.addItem(userId, new AddCartItemUseCase.Command(
                productId,
                "SKU-SOFT-DELETE",
                "Soft Delete Product",
                BigDecimal.TEN,
                "USD",
                1
        ));

        Cart cartAfterRemove = removeCartItemUseCase.removeItem(userId, productId);

        assertThat(cartAfterRemove.items()).isEmpty();
        assertThat(getCartUseCase.getCart(userId).items()).isEmpty();

        CartEntity entity = cartRepository.findByUserId(userId).orElseThrow();
        assertThat(entity.getItems()).hasSize(1);

        CartItemEntity deletedItem = entity.getItems().getFirst();
        assertThat(deletedItem.getProductId()).isEqualTo(productId);
        assertThat(deletedItem.getDeletedAt()).isNotNull();
        assertThat(deletedItem.getDeletedBy()).isEqualTo(userId);
    }
}
