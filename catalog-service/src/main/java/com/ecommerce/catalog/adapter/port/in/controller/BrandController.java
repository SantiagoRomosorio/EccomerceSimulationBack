package com.ecommerce.catalog.adapter.port.in.controller;

import com.ecommerce.catalog.adapter.port.in.dto.BrandResponse;
import com.ecommerce.catalog.adapter.port.in.dto.CreateBrandRequest;
import com.ecommerce.catalog.adapter.port.in.mapper.CatalogDtoMapper;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.catalog.application.port.in.CreateBrandUseCase;
import com.ecommerce.catalog.application.port.in.ListBrandsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
@RequestMapping("/api/brands")
public class BrandController {

    private final CreateBrandUseCase createBrandUseCase;
    private final ListBrandsUseCase listBrandsUseCase;
    private final CatalogDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public BrandController(
            CreateBrandUseCase createBrandUseCase,
            ListBrandsUseCase listBrandsUseCase,
            CatalogDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.createBrandUseCase = createBrandUseCase;
        this.listBrandsUseCase = listBrandsUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping
    @Operation(summary = "Create a product brand")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Brand created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Brand slug already exists"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<BrandResponse>> create(
            @Valid @RequestBody CreateBrandRequest body,
            HttpServletRequest request
    ) {
        BrandResponse response = mapper.toResponse(createBrandUseCase.create(new CreateBrandUseCase.Command(
                body.name(),
                body.slug(),
                body.description()
        )));

        return responseFactory.success(ApiStatusCode.CREATED, "Brand created successfully", response, request);
    }

    @GetMapping
    @Operation(summary = "List product brands")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Brands returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<BrandResponse>>> list(HttpServletRequest request) {
        List<BrandResponse> response = listBrandsUseCase.listBrands().stream()
                .map(mapper::toResponse)
                .toList();

        return responseFactory.success(ApiStatusCode.OK, "Brands returned successfully", response, request);
    }
}
