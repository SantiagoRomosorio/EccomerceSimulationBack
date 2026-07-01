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
import com.ecommerce.catalog.domain.exception.ConflictException;
import com.ecommerce.catalog.domain.exception.ResourceNotFoundException;
import com.ecommerce.catalog.domain.model.Brand;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.Product;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.transaction.annotation.Transactional;

public class CatalogService implements CreateCategoryUseCase, ListCategoriesUseCase,
        CreateBrandUseCase, ListBrandsUseCase,
        CreateProductUseCase, GetProductUseCase, ListProductsUseCase, UpdateProductStockUseCase,
        ReserveProductStockUseCase, ReleaseProductStockUseCase {

    private final CategoryRepositoryPort categoryRepository;
    private final BrandRepositoryPort brandRepository;
    private final ProductRepositoryPort productRepository;

    public CatalogService(
            CategoryRepositoryPort categoryRepository,
            BrandRepositoryPort brandRepository,
            ProductRepositoryPort productRepository
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
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
    public Product updateStock(UUID productId, UpdateProductStockUseCase.Command command) {
        Product product = getById(productId).withStockQuantity(command.stockQuantity());
        return productRepository.save(product);
    }

    @Override
    @Transactional
    public List<Product> reserveStock(List<ReserveProductStockUseCase.Command> commands) {
        return commands.stream()
                .map(this::reserveStock)
                .toList();
    }

    private Product reserveStock(ReserveProductStockUseCase.Command command) {
        Product product = productRepository.findProductByIdForUpdate(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found", Map.of("productId", command.productId())));

        if (product.stockQuantity() < command.quantity()) {
            throw new ConflictException("Insufficient product stock", Map.of(
                    "productId", product.id(),
                    "requestedQuantity", command.quantity(),
                    "availableQuantity", product.stockQuantity()
            ));
        }

        return productRepository.save(product.withStockQuantity(product.stockQuantity() - command.quantity()));
    }

    @Override
    @Transactional
    public List<Product> releaseStock(List<ReleaseProductStockUseCase.Command> commands) {
        return commands.stream()
                .map(this::releaseStock)
                .toList();
    }

    private Product releaseStock(ReleaseProductStockUseCase.Command command) {
        Product product = productRepository.findProductByIdForUpdate(command.productId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found", Map.of("productId", command.productId())));

        return productRepository.save(product.withStockQuantity(product.stockQuantity() + command.quantity()));
    }
}
