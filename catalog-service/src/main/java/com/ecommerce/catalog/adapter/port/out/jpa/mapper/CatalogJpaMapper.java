package com.ecommerce.catalog.adapter.port.out.jpa.mapper;

import com.ecommerce.catalog.adapter.port.out.jpa.entity.BrandEntity;
import com.ecommerce.catalog.adapter.port.out.jpa.entity.CategoryEntity;
import com.ecommerce.catalog.adapter.port.out.jpa.entity.ProductEntity;
import com.ecommerce.catalog.domain.model.Brand;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.Product;
import org.springframework.stereotype.Component;

@Component
public class CatalogJpaMapper {

    public CategoryEntity toEntity(Category category) {
        CategoryEntity entity = new CategoryEntity();
        entity.setId(category.id());
        entity.setName(category.name());
        entity.setSlug(category.slug());
        entity.setDescription(category.description());
        entity.setActive(category.active());
        entity.setCreatedAt(category.createdAt());
        entity.setUpdatedAt(category.updatedAt());
        return entity;
    }

    public Category toDomain(CategoryEntity entity) {
        return new Category(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public BrandEntity toEntity(Brand brand) {
        BrandEntity entity = new BrandEntity();
        entity.setId(brand.id());
        entity.setName(brand.name());
        entity.setSlug(brand.slug());
        entity.setDescription(brand.description());
        entity.setActive(brand.active());
        entity.setCreatedAt(brand.createdAt());
        entity.setUpdatedAt(brand.updatedAt());
        return entity;
    }

    public Brand toDomain(BrandEntity entity) {
        return new Brand(
                entity.getId(),
                entity.getName(),
                entity.getSlug(),
                entity.getDescription(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    public ProductEntity toEntity(Product product) {
        ProductEntity entity = new ProductEntity();
        entity.setId(product.id());
        entity.setSku(product.sku());
        entity.setName(product.name());
        entity.setDescription(product.description());
        entity.setPrice(product.price());
        entity.setCurrency(product.currency());
        entity.setCategoryId(product.categoryId());
        entity.setCategoryName(product.categoryName());
        entity.setBrandId(product.brandId());
        entity.setBrandName(product.brandName());
        entity.setStockQuantity(product.stockQuantity());
        entity.setActive(product.active());
        entity.setCreatedAt(product.createdAt());
        entity.setUpdatedAt(product.updatedAt());
        return entity;
    }

    public Product toDomain(ProductEntity entity) {
        return new Product(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getDescription(),
                entity.getPrice(),
                entity.getCurrency(),
                entity.getCategoryId(),
                entity.getCategoryName(),
                entity.getBrandId(),
                entity.getBrandName(),
                entity.getStockQuantity(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
