package com.ecommerce.catalog.adapter.port.out.jpa;

import com.ecommerce.catalog.adapter.port.out.jpa.mapper.CatalogJpaMapper;
import com.ecommerce.catalog.adapter.port.out.jpa.repository.BrandJpaRepository;
import com.ecommerce.catalog.adapter.port.out.jpa.repository.CategoryJpaRepository;
import com.ecommerce.catalog.adapter.port.out.jpa.repository.ProductJpaRepository;
import com.ecommerce.catalog.application.port.out.BrandRepositoryPort;
import com.ecommerce.catalog.application.port.out.CategoryRepositoryPort;
import com.ecommerce.catalog.application.port.out.ProductRepositoryPort;
import com.ecommerce.catalog.domain.model.Brand;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.Product;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class CatalogPersistenceAdapter implements CategoryRepositoryPort, BrandRepositoryPort, ProductRepositoryPort {

    private final CategoryJpaRepository categoryRepository;
    private final BrandJpaRepository brandRepository;
    private final ProductJpaRepository productRepository;
    private final CatalogJpaMapper mapper;

    public CatalogPersistenceAdapter(
            CategoryJpaRepository categoryRepository,
            BrandJpaRepository brandRepository,
            ProductJpaRepository productRepository,
            CatalogJpaMapper mapper
    ) {
        this.categoryRepository = categoryRepository;
        this.brandRepository = brandRepository;
        this.productRepository = productRepository;
        this.mapper = mapper;
    }

    @Override
    public Category save(Category category) {
        return mapper.toDomain(categoryRepository.save(mapper.toEntity(category)));
    }

    @Override
    public List<Category> findAllCategories() {
        return categoryRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Category> findCategoryById(UUID id) {
        return categoryRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsCategoryBySlug(String slug) {
        return categoryRepository.existsBySlug(slug);
    }

    @Override
    public Brand save(Brand brand) {
        return mapper.toDomain(brandRepository.save(mapper.toEntity(brand)));
    }

    @Override
    public List<Brand> findAllBrands() {
        return brandRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Brand> findBrandById(UUID id) {
        return brandRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsBrandBySlug(String slug) {
        return brandRepository.existsBySlug(slug);
    }

    @Override
    public Product save(Product product) {
        return mapper.toDomain(productRepository.save(mapper.toEntity(product)));
    }

    @Override
    public List<Product> findAllProducts() {
        return productRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public Optional<Product> findProductById(UUID id) {
        return productRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Product> findProductByIdForUpdate(UUID id) {
        return productRepository.findWithLockingById(id).map(mapper::toDomain);
    }

    @Override
    public boolean existsBySku(String sku) {
        return productRepository.existsBySku(sku);
    }
}
