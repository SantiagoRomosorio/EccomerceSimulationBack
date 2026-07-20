package com.ecommerce.commerce.adapter.port.in.controller;

import static com.ecommerce.common.web.openapi.OpenApiSecurity.BEARER_AUTH;

import com.ecommerce.commerce.adapter.port.in.dto.AddCartItemRequest;
import com.ecommerce.commerce.adapter.port.in.dto.CartResponse;
import com.ecommerce.commerce.adapter.port.in.dto.UpdateCartItemQuantityRequest;
import com.ecommerce.commerce.adapter.port.in.mapper.CommerceDtoMapper;
import com.ecommerce.commerce.application.port.in.AddCartItemUseCase;
import com.ecommerce.commerce.application.port.in.GetCartUseCase;
import com.ecommerce.commerce.application.port.in.RemoveCartItemUseCase;
import com.ecommerce.commerce.application.port.in.UpdateCartItemQuantityUseCase;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
@SecurityRequirement(name = BEARER_AUTH)
public class CartController {

    private final GetCartUseCase getCartUseCase;
    private final AddCartItemUseCase addCartItemUseCase;
    private final UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase;
    private final RemoveCartItemUseCase removeCartItemUseCase;
    private final CommerceDtoMapper mapper;
    private final ApiResponseFactory responseFactory;

    public CartController(
            GetCartUseCase getCartUseCase,
            AddCartItemUseCase addCartItemUseCase,
            UpdateCartItemQuantityUseCase updateCartItemQuantityUseCase,
            RemoveCartItemUseCase removeCartItemUseCase,
            CommerceDtoMapper mapper,
            ApiResponseFactory responseFactory
    ) {
        this.getCartUseCase = getCartUseCase;
        this.addCartItemUseCase = addCartItemUseCase;
        this.updateCartItemQuantityUseCase = updateCartItemQuantityUseCase;
        this.removeCartItemUseCase = removeCartItemUseCase;
        this.mapper = mapper;
        this.responseFactory = responseFactory;
    }

    @GetMapping
    @Operation(summary = "Get current user cart")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<CartResponse>> getCart(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request
    ) {
        return responseFactory.success(
                ApiStatusCode.OK,
                "Cart returned successfully",
                mapper.toResponse(getCartUseCase.getCart(userId)),
                request
        );
    }

    @PostMapping("/items")
    @Operation(summary = "Add item to current user cart")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cart item added"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<CartResponse>> addItem(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AddCartItemRequest body,
            HttpServletRequest request
    ) {
        CartResponse response = mapper.toResponse(addCartItemUseCase.addItem(userId, new AddCartItemUseCase.Command(
                body.productId(),
                body.quantity()
        )));

        return responseFactory.success(ApiStatusCode.CREATED, "Cart item added successfully", response, request);
    }

    @PatchMapping("/items/{productId}")
    @Operation(summary = "Update current user cart item quantity")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body or product id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<CartResponse>> updateQuantity(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID productId,
            @Valid @RequestBody UpdateCartItemQuantityRequest body,
            HttpServletRequest request
    ) {
        CartResponse response = mapper.toResponse(updateCartItemQuantityUseCase.updateQuantity(
                userId,
                productId,
                new UpdateCartItemQuantityUseCase.Command(body.quantity())
        ));

        return responseFactory.success(ApiStatusCode.OK, "Cart item updated successfully", response, request);
    }

    @DeleteMapping("/items/{productId}")
    @Operation(summary = "Remove item from current user cart")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Cart item removed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid product id"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing authenticated user header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Cart item not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<CartResponse>> removeItem(
            @RequestHeader("X-User-Id") UUID userId,
            @PathVariable UUID productId,
            HttpServletRequest request
    ) {
        CartResponse response = mapper.toResponse(removeCartItemUseCase.removeItem(userId, productId));
        return responseFactory.success(ApiStatusCode.OK, "Cart item removed successfully", response, request);
    }
}
