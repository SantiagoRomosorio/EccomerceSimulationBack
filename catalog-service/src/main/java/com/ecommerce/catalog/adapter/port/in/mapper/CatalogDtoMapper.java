package com.ecommerce.catalog.adapter.port.in.mapper;

import com.ecommerce.catalog.adapter.port.in.dto.BrandResponse;
import com.ecommerce.catalog.adapter.port.in.dto.CategoryResponse;
import com.ecommerce.catalog.adapter.port.in.dto.ProductResponse;
import com.ecommerce.catalog.domain.model.Brand;
import com.ecommerce.catalog.domain.model.Category;
import com.ecommerce.catalog.domain.model.Product;
import org.springframework.stereotype.Component;

@Component
public class CatalogDtoMapper {

    public CategoryResponse toResponse(Category category) {
        return new CategoryResponse(
                category.id(),
                category.name(),
                category.slug(),
                category.description(),
                category.active(),
                category.createdAt(),
                category.updatedAt()
        );
    }

    public BrandResponse toResponse(Brand brand) {
        return new BrandResponse(
                brand.id(),
                brand.name(),
                brand.slug(),
                brand.description(),
                brand.active(),
                brand.createdAt(),
                brand.updatedAt()
        );
    }

    public ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.id(),
                product.sku(),
                product.name(),
                product.description(),
                product.price(),
                product.currency(),
                product.categoryId(),
                product.categoryName(),
                product.brandId(),
                product.brandName(),
                product.stockQuantity(),
                product.active(),
                product.createdAt(),
                product.updatedAt()
        );
    }
}
