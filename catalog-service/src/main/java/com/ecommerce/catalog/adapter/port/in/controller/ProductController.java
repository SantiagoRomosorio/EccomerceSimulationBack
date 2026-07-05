package com.ecommerce.catalog.adapter.port.in.controller;

import static com.ecommerce.common.web.openapi.OpenApiSecurity.BEARER_AUTH;

import com.ecommerce.catalog.adapter.port.in.dto.CreateProductRequest;
import com.ecommerce.catalog.adapter.port.in.dto.ProductResponse;
import com.ecommerce.catalog.adapter.port.in.dto.UpdateProductStockRequest;
import com.ecommerce.catalog.adapter.port.in.mapper.CatalogDtoMapper;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.catalog.application.port.in.CreateProductUseCase;
import com.ecommerce.catalog.application.port.in.GetProductUseCase;
import com.ecommerce.catalog.application.port.in.ListProductsUseCase;
import com.ecommerce.catalog.application.port.in.UpdateProductStockUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@SecurityRequirement(name = BEARER_AUTH)
public class ProductController {

    private final CreateProductUseCase createProductUseCase;
    private final GetProductUseCase getProductUseCase;
    private final ListProductsUseCase listProductsUseCase;
    private final UpdateProductStockUseCase updateProductStockUseCase;
    private final CatalogDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public ProductController(
            CreateProductUseCase createProductUseCase,
            GetProductUseCase getProductUseCase,
            ListProductsUseCase listProductsUseCase,
            UpdateProductStockUseCase updateProductStockUseCase,
            CatalogDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.createProductUseCase = createProductUseCase;
        this.getProductUseCase = getProductUseCase;
        this.listProductsUseCase = listProductsUseCase;
        this.updateProductStockUseCase = updateProductStockUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    @Operation(summary = "Create a product with basic stock")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Product created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Category or brand not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Product SKU already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> create(
            @Valid @RequestBody CreateProductRequest body,
            HttpServletRequest request
    ) {
        ProductResponse response = mapper.toResponse(createProductUseCase.create(new CreateProductUseCase.Command(
                body.sku(),
                body.name(),
                body.description(),
                body.price(),
                body.currency(),
                body.categoryId(),
                body.brandId(),
                body.stockQuantity()
        )));

        return responseFactory.success(ApiStatusCode.CREATED, "Product created successfully", response, request);
    }

    @GetMapping
    @Operation(summary = "List products")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<ProductResponse>>> list(HttpServletRequest request) {
        List<ProductResponse> response = listProductsUseCase.listProducts().stream()
                .map(mapper::toResponse)
                .toList();

        return responseFactory.success(ApiStatusCode.OK, "Products returned successfully", response, request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a product by id")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> getById(
            @PathVariable UUID id,
            HttpServletRequest request
    ) {
        ProductResponse response = mapper.toResponse(getProductUseCase.getById(id));
        return responseFactory.success(ApiStatusCode.OK, "Product returned successfully", response, request);
    }

    @PatchMapping("/{id}/stock")
    @Operation(summary = "Update basic product stock")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product stock updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or product id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<ProductResponse>> updateStock(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductStockRequest body,
            HttpServletRequest request
    ) {
        ProductResponse response = mapper.toResponse(updateProductStockUseCase.updateStock(
                id,
                new UpdateProductStockUseCase.Command(body.stockQuantity())
        ));

        return responseFactory.success(ApiStatusCode.OK, "Product stock updated successfully", response, request);
    }
}
