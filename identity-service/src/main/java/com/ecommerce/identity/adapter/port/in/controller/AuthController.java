package com.ecommerce.identity.adapter.port.in.controller;

import com.ecommerce.identity.adapter.port.in.dto.AuthResponse;
import com.ecommerce.identity.adapter.port.in.dto.LoginRequest;
import com.ecommerce.identity.adapter.port.in.dto.RegisterUserRequest;
import com.ecommerce.identity.adapter.port.in.dto.UserResponse;
import com.ecommerce.identity.adapter.port.in.mapper.UserDtoMapper;
import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import com.ecommerce.identity.application.port.in.LoginUseCase;
import com.ecommerce.identity.application.port.in.RegisterUserUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final LoginUseCase loginUseCase;
    private final UserDtoMapper userDtoMapper;
    private final ApiResponseFactory responseFactory;

    public AuthController(
            RegisterUserUseCase registerUserUseCase,
            LoginUseCase loginUseCase,
            UserDtoMapper userDtoMapper,
            ApiResponseFactory responseFactory
    ) {
        this.registerUserUseCase = registerUserUseCase;
        this.loginUseCase = loginUseCase;
        this.userDtoMapper = userDtoMapper;
        this.responseFactory = responseFactory;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a customer user")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "User registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Email already registered"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody RegisterUserRequest request,
            HttpServletRequest servletRequest
    ) {
        UserResponse response = userDtoMapper.toResponse(registerUserUseCase.register(new RegisterUserUseCase.Command(
                request.email(),
                request.fullName(),
                request.password()
        )));

        return responseFactory.success(
                ApiStatusCode.CREATED,
                "User registered successfully",
                response,
                servletRequest
        );
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate a user and emit a JWT")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login completed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request body"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "Unexpected server error")
    })
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest servletRequest
    ) {
        LoginUseCase.Result result = loginUseCase.login(new LoginUseCase.Command(
                request.email(),
                request.password()
        ));

        AuthResponse response = new AuthResponse(result.accessToken(), result.tokenType(), result.expiresInMinutes());

        return responseFactory.success(
                ApiStatusCode.OK,
                "Login completed successfully",
                response,
                servletRequest
        );
    }
}
