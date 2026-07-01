package com.ecommerce.catalog.adapter.port.in.controller;

import com.ecommerce.catalog.adapter.port.in.dto.ProductResponse;
import com.ecommerce.catalog.adapter.port.in.dto.ReserveProductStockRequest;
import com.ecommerce.catalog.adapter.port.in.mapper.CatalogDtoMapper;
import com.ecommerce.catalog.application.port.in.ReserveProductStockUseCase;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/products/stock")
public class InternalProductStockController {

    private final ReserveProductStockUseCase reserveProductStockUseCase;
    private final CatalogDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public InternalProductStockController(
            ReserveProductStockUseCase reserveProductStockUseCase,
            CatalogDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.reserveProductStockUseCase = reserveProductStockUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/reservations")
    @Operation(summary = "Reserve product stock for an internal checkout")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Product stock reserved"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Product not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Insufficient stock"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<List<ProductResponse>>> reserve(
            @Valid @RequestBody ReserveProductStockRequest body,
            HttpServletRequest request
    ) {
        List<ProductResponse> response = reserveProductStockUseCase.reserveStock(body.items().stream()
                        .map(item -> new ReserveProductStockUseCase.Command(item.productId(), item.quantity()))
                        .toList())
                .stream()
                .map(mapper::toResponse)
                .toList();

        return responseFactory.success(ApiStatusCode.OK, "Product stock reserved successfully", response, request);
    }
}
