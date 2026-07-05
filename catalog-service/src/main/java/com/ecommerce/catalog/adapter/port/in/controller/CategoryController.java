package com.ecommerce.catalog.adapter.port.in.controller;

import static com.ecommerce.common.web.openapi.OpenApiSecurity.BEARER_AUTH;

import com.ecommerce.catalog.adapter.port.in.dto.CategoryResponse;
import com.ecommerce.catalog.adapter.port.in.dto.CreateCategoryRequest;
import com.ecommerce.catalog.adapter.port.in.mapper.CatalogDtoMapper;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.catalog.application.port.in.CreateCategoryUseCase;
import com.ecommerce.catalog.application.port.in.ListCategoriesUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@SecurityRequirement(name = BEARER_AUTH)
public class CategoryController {

    private final CreateCategoryUseCase createCategoryUseCase;
    private final ListCategoriesUseCase listCategoriesUseCase;
    private final CatalogDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public CategoryController(
            CreateCategoryUseCase createCategoryUseCase,
            ListCategoriesUseCase listCategoriesUseCase,
            CatalogDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.createCategoryUseCase = createCategoryUseCase;
        this.listCategoriesUseCase = listCategoriesUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    @Operation(summary = "Create a product category")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Category created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Category slug already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<CategoryResponse>> create(
            @Valid @RequestBody CreateCategoryRequest body,
            HttpServletRequest request
    ) {
        CategoryResponse response = mapper.toResponse(createCategoryUseCase.create(new CreateCategoryUseCase.Command(
                body.name(),
                body.slug(),
                body.description()
        )));

        return responseFactory.success(ApiStatusCode.CREATED, "Category created successfully", response, request);
    }

    @GetMapping
    @Operation(summary = "List product categories")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Categories returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> list(HttpServletRequest request) {
        List<CategoryResponse> response = listCategoriesUseCase.listCategories().stream()
                .map(mapper::toResponse)
                .toList();

        return responseFactory.success(ApiStatusCode.OK, "Categories returned successfully", response, request);
    }
}
