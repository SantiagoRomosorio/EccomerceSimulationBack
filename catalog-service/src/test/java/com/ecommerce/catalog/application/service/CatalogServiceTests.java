package com.ecommerce.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.catalog.application.port.in.ReleaseProductStockUseCase;
import com.ecommerce.catalog.application.port.out.BrandRepositoryPort;
import com.ecommerce.catalog.application.port.out.CategoryRepositoryPort;
import com.ecommerce.catalog.application.port.out.ProductRepositoryPort;
import com.ecommerce.catalog.domain.exception.ConflictException;
import com.ecommerce.catalog.domain.model.Product;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CatalogServiceTests {

    @Mock
    private CategoryRepositoryPort categoryRepository;

    @Mock
    private BrandRepositoryPort brandRepository;

    @Mock
    private ProductRepositoryPort productRepository;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(categoryRepository, brandRepository, productRepository);
    }

    @Test
    void rejectsReleasedStockAboveMaximum() {
        Product product = productWithStock(1_000_000);
        when(productRepository.findProductByIdForUpdate(product.id())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.releaseStock(List.of(
                new ReleaseProductStockUseCase.Command(product.id(), 1)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product stock quantity exceeds maximum");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void convertsReleasedStockOverflowIntoControlledDomainError() {
        Product product = productWithStock(Integer.MAX_VALUE);
        when(productRepository.findProductByIdForUpdate(product.id())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.releaseStock(List.of(
                new ReleaseProductStockUseCase.Command(product.id(), 1)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product stock quantity exceeds maximum");

        verify(productRepository, never()).save(any(Product.class));
    }

    private Product productWithStock(int stockQuantity) {
        Instant now = Instant.now();
        return new Product(
                UUID.randomUUID(),
                "SKU-001",
                "Product",
                null,
                BigDecimal.ONE,
                "USD",
                UUID.randomUUID(),
                "Category",
                UUID.randomUUID(),
                "Brand",
                stockQuantity,
                true,
                now,
                now
        );
    }
}
