package com.ecommerce.catalog.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ecommerce.catalog.application.port.in.ReserveProductStockUseCase;
import com.ecommerce.catalog.application.port.out.BrandRepositoryPort;
import com.ecommerce.catalog.application.port.out.CategoryRepositoryPort;
import com.ecommerce.catalog.application.port.out.ProductRepositoryPort;
import com.ecommerce.catalog.application.port.out.StockReservationRepositoryPort;
import com.ecommerce.catalog.domain.exception.ConflictException;
import com.ecommerce.catalog.domain.exception.ResourceNotFoundException;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.StockReservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
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

    @Mock
    private StockReservationRepositoryPort stockReservationRepository;

    private CatalogService service;

    @BeforeEach
    void setUp() {
        service = new CatalogService(
                categoryRepository,
                brandRepository,
                productRepository,
                stockReservationRepository
        );
    }

    @Test
    void rejectsReleasedStockAboveMaximum() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(1_000_000);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(reserved(reservationId, product.id(), 1)));
        when(productRepository.findProductByIdForUpdate(product.id())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.releaseStock(reservationId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product stock quantity exceeds maximum");

        verify(productRepository, never()).save(any(Product.class));
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void convertsReleasedStockOverflowIntoControlledDomainError() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(Integer.MAX_VALUE);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(reserved(reservationId, product.id(), 1)));
        when(productRepository.findProductByIdForUpdate(product.id())).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> service.releaseStock(reservationId))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Product stock quantity exceeds maximum");

        verify(productRepository, never()).save(any(Product.class));
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void normalizesDuplicateItemsAndLocksProductsInUuidOrder() {
        UUID reservationId = UUID.randomUUID();
        UUID firstProductId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        UUID secondProductId = UUID.fromString("00000000-0000-0000-0000-000000000002");
        Product firstProduct = productWithIdAndStock(firstProductId, 10);
        Product secondProduct = productWithIdAndStock(secondProductId, 10);

        when(stockReservationRepository.claim(reservationId)).thenReturn(true);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(new StockReservation(
                        reservationId,
                        StockReservation.Status.PENDING,
                        List.of()
                )));
        when(productRepository.findProductByIdForUpdate(firstProductId)).thenReturn(Optional.of(firstProduct));
        when(productRepository.findProductByIdForUpdate(secondProductId)).thenReturn(Optional.of(secondProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(stockReservationRepository.save(any(StockReservation.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        List<Product> result = service.reserveStock(reservationId, List.of(
                new ReserveProductStockUseCase.Command(secondProductId, 1),
                new ReserveProductStockUseCase.Command(firstProductId, 2),
                new ReserveProductStockUseCase.Command(secondProductId, 3)
        ));

        assertThat(result)
                .extracting(Product::stockQuantity)
                .containsExactly(8, 6);

        InOrder lockOrder = inOrder(productRepository);
        lockOrder.verify(productRepository).findProductByIdForUpdate(firstProductId);
        lockOrder.verify(productRepository).findProductByIdForUpdate(secondProductId);

        ArgumentCaptor<StockReservation> reservationCaptor =
                ArgumentCaptor.forClass(StockReservation.class);
        verify(stockReservationRepository).save(reservationCaptor.capture());
        assertThat(reservationCaptor.getValue().status()).isEqualTo(StockReservation.Status.RESERVED);
        assertThat(reservationCaptor.getValue().items()).containsExactly(
                new StockReservation.Item(firstProductId, 2),
                new StockReservation.Item(secondProductId, 4)
        );
    }

    @Test
    void identicalReservationRetryDoesNotChangeStock() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(7);
        StockReservation reservation = reserved(reservationId, product.id(), 3);

        when(stockReservationRepository.claim(reservationId)).thenReturn(false);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(reservation));
        when(productRepository.findProductById(product.id())).thenReturn(Optional.of(product));

        List<Product> result = service.reserveStock(reservationId, List.of(
                new ReserveProductStockUseCase.Command(product.id(), 3)
        ));

        assertThat(result).containsExactly(product);
        verify(productRepository, never()).findProductByIdForUpdate(any(UUID.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void reusedReservationIdWithDifferentPayloadReturnsConflict() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(7);

        when(stockReservationRepository.claim(reservationId)).thenReturn(false);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(reserved(reservationId, product.id(), 3)));

        assertThatThrownBy(() -> service.reserveStock(reservationId, List.of(
                new ReserveProductStockUseCase.Command(product.id(), 4)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Stock reservation payload does not match existing reservation");

        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void releasedReservationCannotBeReservedAgain() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(10);
        StockReservation released = new StockReservation(
                reservationId,
                StockReservation.Status.RELEASED,
                List.of(new StockReservation.Item(product.id(), 3))
        );

        when(stockReservationRepository.claim(reservationId)).thenReturn(false);
        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(released));

        assertThatThrownBy(() -> service.reserveStock(reservationId, List.of(
                new ReserveProductStockUseCase.Command(product.id(), 3)
        )))
                .isInstanceOf(ConflictException.class)
                .hasMessage("Stock reservation has already been released");

        verify(productRepository, never()).findProductById(any(UUID.class));
        verify(productRepository, never()).save(any(Product.class));
    }

    @Test
    void repeatedReleaseDoesNotRestoreStockTwice() {
        UUID reservationId = UUID.randomUUID();
        Product product = productWithStock(10);
        StockReservation released = new StockReservation(
                reservationId,
                StockReservation.Status.RELEASED,
                List.of(new StockReservation.Item(product.id(), 3))
        );

        when(stockReservationRepository.findByIdForUpdate(reservationId))
                .thenReturn(Optional.of(released));
        when(productRepository.findProductById(product.id())).thenReturn(Optional.of(product));

        assertThat(service.releaseStock(reservationId)).containsExactly(product);
        verify(productRepository, never()).findProductByIdForUpdate(any(UUID.class));
        verify(productRepository, never()).save(any(Product.class));
        verify(stockReservationRepository, never()).save(any(StockReservation.class));
    }

    @Test
    void releaseUnknownReservationReturnsNotFound() {
        UUID reservationId = UUID.randomUUID();
        when(stockReservationRepository.findByIdForUpdate(reservationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.releaseStock(reservationId))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("Stock reservation not found");

        verify(productRepository, never()).save(any(Product.class));
    }

    private Product productWithStock(int stockQuantity) {
        return productWithIdAndStock(UUID.randomUUID(), stockQuantity);
    }

    private Product productWithIdAndStock(UUID productId, int stockQuantity) {
        Instant now = Instant.now();
        return new Product(
                productId,
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

    private StockReservation reserved(UUID reservationId, UUID productId, int quantity) {
        return new StockReservation(
                reservationId,
                StockReservation.Status.RESERVED,
                List.of(new StockReservation.Item(productId, quantity))
        );
    }
}
