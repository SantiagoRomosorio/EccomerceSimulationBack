package com.ecommerce.identity.adapter.port.in.controller;

import com.ecommerce.identity.adapter.port.in.dto.UserResponse;
import com.ecommerce.identity.adapter.port.in.mapper.UserDtoMapper;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.identity.application.port.in.GetCurrentUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final GetCurrentUserUseCase getCurrentUserUseCase;
    private final UserDtoMapper userDtoMapper;
    private final ApiResponseFactory responseFactory;

    public UserController(
            GetCurrentUserUseCase getCurrentUserUseCase,
            UserDtoMapper userDtoMapper,
            ApiResponseFactory responseFactory
    ) {
        this.getCurrentUserUseCase = getCurrentUserUseCase;
        this.userDtoMapper = userDtoMapper;
        this.responseFactory = responseFactory;
    }

    @GetMapping("/me")
    @Operation(summary = "Return the authenticated user profile")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Current user returned"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid trusted user id header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Missing trusted identity header"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "User not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<UserResponse>> me(
            @RequestHeader("X-User-Id") UUID userId,
            HttpServletRequest request
    ) {
        UserResponse response = userDtoMapper.toResponse(getCurrentUserUseCase.getById(userId));

        return responseFactory.success(
                ApiStatusCode.OK,
                "Current user returned successfully",
                response,
                request
        );
    }
}
