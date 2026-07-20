package com.ecommerce.catalog.application.service;

import com.ecommerce.catalog.application.port.in.CreateBrandUseCase;
import com.ecommerce.catalog.application.port.in.CreateCategoryUseCase;
import com.ecommerce.catalog.application.port.in.CreateProductUseCase;
import com.ecommerce.catalog.application.port.in.GetProductUseCase;
import com.ecommerce.catalog.application.port.in.ListBrandsUseCase;
import com.ecommerce.catalog.application.port.in.ListCategoriesUseCase;
import com.ecommerce.catalog.application.port.in.ListProductsUseCase;
import com.ecommerce.catalog.application.port.in.ReleaseProductStockUseCase;
import com.ecommerce.catalog.application.port.in.ReserveProductStockUseCase;
import com.ecommerce.catalog.application.port.in.UpdateProductStockUseCase;
import com.ecommerce.catalog.application.port.out.BrandRepositoryPort;
import com.ecommerce.catalog.application.port.out.CategoryRepositoryPort;
import com.ecommerce.catalog.application.port.out.ProductRepositoryPort;
import com.ecommerce.catalog.application.port.out.StockReservationRepositoryPort;
import com.ecommerce.catalog.domain.exception.ConflictException;
import com.ecommerce.catalog.domain.exception.InvalidStockReservationException;
import com.ecommerce.catalog.domain.exception.ResourceNotFoundException;
import com.ecommerce.catalog.domain.model.Brand;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.Product;
import com.ecommerce.catalog.domain.model.StockReservation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CatalogService implements CreateCategoryUseCase, ListCategoriesUseCase,
        CreateBrandUseCase, ListBrandsUseCase,
        CreateProductUseCase, GetProductUseCase, ListProductsUseCase, UpdateProductStockUseCase,
        ReserveProductStockUseCase, ReleaseProductStockUseCase {

    private static final int MAX_STOCK_QUANTITY = 1_000_000;
    private static final int MAX_RESERVATION_QUANTITY = 1_000;

    private final CategoryRepositoryPort categoryRepository;
    private final BrandRepositoryPort brandRepository;
    private final ProductRepositoryPort productRepository;
    private final StockReservationRepositoryPort stockReservationRepository;

    public CatalogService(
            CategoryRepositoryPort categoryRepository,
            BrandRepositoryPort brandRepository,
            ProductRepositoryPort productRepository,
            StockReservationRepositoryPort stockReservationRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.stockReservationRepository = stockReservationRepository;
    }

    @Override
    public Category create(CreateCategoryUseCase.Command command) {
        if (categoryRepository.existsCategoryBySlug(command.slug())) {
            throw new ConflictException("Category slug is already registered", Map.of("slug", command.slug()));
        }

        return categoryRepository.save(Category.create(command.name(), command.slug(), command.description()));
    }

    @Override
    public List<Category> listCategories() {
        return categoryRepository.findAllCategories();
    }

    @Override
    public Brand create(CreateBrandUseCase.Command command) {
        if (brandRepository.existsBrandBySlug(command.slug())) {
            throw new ConflictException("Brand slug is already registered", Map.of("slug", command.slug()));
        }

        return brandRepository.save(Brand.create(command.name(), command.slug(), command.description()));
    }

    @Override
    public List<Brand> listBrands() {
        return brandRepository.findAllBrands();
    }

    @Override
    public Product create(CreateProductUseCase.Command command) {
        if (productRepository.existsBySku(command.sku())) {
            throw new ConflictException("Product SKU is already registered", Map.of("sku", command.sku()));
        }

        Category category = categoryRepository.findCategoryById(command.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category not found", Map.of("categoryId", command.categoryId())));
        Brand brand = brandRepository.findBrandById(command.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand not found", Map.of("brandId", command.brandId())));

        Product product = Product.create(
                command.sku(),
                command.name(),
                command.description(),
                command.price(),
                command.currency(),
                category,
                brand,
                command.stockQuantity()
        );

        return productRepository.save(product);
    }

    @Override
    public Product getById(UUID id) {
        return productRepository.findProductById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found", Map.of("productId", id)));
    }

    @Override
    public List<Product> listProducts() {
        return productRepository.findAllProducts();
    }

    @Override
    @Transactional
    public Product updateStock(UUID productId, UpdateProductStockUseCase.Command command) {
        Product product = productRepository.findProductByIdForUpdate(productId)
                .orElseThrow(() -> productNotFound(productId))
                .withStockQuantity(command.stockQuantity());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public List<Product> reserveStock(
            UUID reservationId,
            List<ReserveProductStockUseCase.Command> commands
    ) {
        validateReservationId(reservationId);
        List<StockReservation.Item> requestedItems = normalize(commands);

        boolean claimed = stockReservationRepository.claim(reservationId);
        StockReservation reservation = stockReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new IllegalStateException("Claimed stock reservation could not be loaded"));

        if (!claimed) {
            assertSamePayload(reservation, requestedItems);
            if (reservation.status() == StockReservation.Status.PENDING) {
                throw reservationPending(reservationId);
            }
            if (reservation.status() == StockReservation.Status.RELEASED) {
                throw new ConflictException(
                        "Stock reservation has already been released",
                        Map.of("reservationId", reservationId)
                );
            }
            return findProducts(requestedItems);
        }

        List<Product> products = lockProducts(requestedItems);
        validateAvailableStock(products, requestedItems);

        List<Product> updatedProducts = new ArrayList<>(products.size());
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            int quantity = requestedItems.get(index).quantity();
            updatedProducts.add(productRepository.save(
                    product.withStockQuantity(product.stockQuantity() - quantity)
            ));
        }

        stockReservationRepository.save(new StockReservation(
                reservationId,
                StockReservation.Status.RESERVED,
                requestedItems
        ));
        return List.copyOf(updatedProducts);
    }

    @Override
    @Transactional
    public List<Product> releaseStock(UUID reservationId) {
        validateReservationId(reservationId);
        StockReservation reservation = stockReservationRepository.findByIdForUpdate(reservationId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Stock reservation not found",
                        Map.of("reservationId", reservationId)
                ));

        if (reservation.status() == StockReservation.Status.RELEASED) {
            return findProducts(reservation.items());
        }
        if (reservation.status() == StockReservation.Status.PENDING) {
            throw reservationPending(reservationId);
        }

        List<Product> products = lockProducts(reservation.items());
        List<Integer> releasedQuantities = validateReleasedStock(products, reservation.items());
        List<Product> updatedProducts = new ArrayList<>(products.size());

        for (int index = 0; index < products.size(); index++) {
            updatedProducts.add(productRepository.save(
                    products.get(index).withStockQuantity(releasedQuantities.get(index))
            ));
        }

        stockReservationRepository.save(reservation.withStatus(StockReservation.Status.RELEASED));
        return List.copyOf(updatedProducts);
    }

    private List<StockReservation.Item> normalize(List<ReserveProductStockUseCase.Command> commands) {
        if (commands == null || commands.isEmpty()) {
            throw new InvalidStockReservationException("Stock reservation items are required", Map.of());
        }

        Map<UUID, Integer> quantitiesByProduct = new TreeMap<>();
        for (ReserveProductStockUseCase.Command command : commands) {
            if (command == null || command.productId() == null || command.quantity() < 1) {
                throw new InvalidStockReservationException(
                        "Stock reservation item is invalid",
                        Map.of("minimumQuantity", 1)
                );
            }

            int normalizedQuantity;
            try {
                normalizedQuantity = Math.addExact(
                        quantitiesByProduct.getOrDefault(command.productId(), 0),
                        command.quantity()
                );
            } catch (ArithmeticException exception) {
                throw reservationQuantityExceeded(command.productId());
            }

            if (normalizedQuantity > MAX_RESERVATION_QUANTITY) {
                throw reservationQuantityExceeded(command.productId());
            }
            quantitiesByProduct.put(command.productId(), normalizedQuantity);
        }

        return quantitiesByProduct.entrySet().stream()
                .map(entry -> new StockReservation.Item(entry.getKey(), entry.getValue()))
                .toList();
    }

    private List<Product> lockProducts(List<StockReservation.Item> items) {
        return items.stream()
                .map(item -> productRepository.findProductByIdForUpdate(item.productId())
                        .orElseThrow(() -> productNotFound(item.productId())))
                .toList();
    }

    private List<Product> findProducts(List<StockReservation.Item> items) {
        return items.stream()
                .map(item -> productRepository.findProductById(item.productId())
                        .orElseThrow(() -> productNotFound(item.productId())))
                .toList();
    }

    private void validateAvailableStock(
            List<Product> products,
            List<StockReservation.Item> items
    ) {
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            int quantity = items.get(index).quantity();
            if (product.stockQuantity() < quantity) {
                throw new ConflictException("Insufficient product stock", Map.of(
                        "productId", product.id(),
                        "requestedQuantity", quantity,
                        "availableQuantity", product.stockQuantity()
                ));
            }
        }
    }

    private List<Integer> validateReleasedStock(
            List<Product> products,
            List<StockReservation.Item> items
    ) {
        List<Integer> releasedQuantities = new ArrayList<>(products.size());
        for (int index = 0; index < products.size(); index++) {
            Product product = products.get(index);
            int quantity = items.get(index).quantity();

            int stockQuantity;
            try {
                stockQuantity = Math.addExact(product.stockQuantity(), quantity);
            } catch (ArithmeticException exception) {
                throw stockLimitExceeded(product, quantity);
            }

            if (stockQuantity > MAX_STOCK_QUANTITY) {
                throw stockLimitExceeded(product, quantity);
            }
            releasedQuantities.add(stockQuantity);
        }
        return List.copyOf(releasedQuantities);
    }

    private void assertSamePayload(
            StockReservation reservation,
            List<StockReservation.Item> requestedItems
    ) {
        if (!reservation.items().equals(requestedItems)) {
            throw new ConflictException("Stock reservation payload does not match existing reservation", Map.of(
                    "reservationId", reservation.reservationId()
            ));
        }
    }

    private void validateReservationId(UUID reservationId) {
        if (reservationId == null) {
            throw new InvalidStockReservationException("Stock reservation ID is required", Map.of());
        }
    }

    private InvalidStockReservationException reservationQuantityExceeded(UUID productId) {
        return new InvalidStockReservationException("Stock reservation quantity exceeds maximum", Map.of(
                "productId", productId,
                "maximumQuantity", MAX_RESERVATION_QUANTITY
        ));
    }

    private ConflictException reservationPending(UUID reservationId) {
        return new ConflictException("Stock reservation is pending", Map.of("reservationId", reservationId));
    }

    private ResourceNotFoundException productNotFound(UUID productId) {
        return new ResourceNotFoundException("Product not found", Map.of("productId", productId));
    }

    private ConflictException stockLimitExceeded(Product product, int quantityToRelease) {
        return new ConflictException("Product stock quantity exceeds maximum", Map.of(
                "productId", product.id(),
                "currentQuantity", product.stockQuantity(),
                "quantityToRelease", quantityToRelease,
                "maximumQuantity", MAX_STOCK_QUANTITY
        ));
    }
}
