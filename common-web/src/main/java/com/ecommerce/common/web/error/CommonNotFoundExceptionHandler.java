package com.ecommerce.common.web.error;

import com.ecommerce.common.web.response.ApiResponse;
import com.ecommerce.common.web.response.ApiResponseFactory;
import com.ecommerce.common.web.response.ApiStatusCode;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CommonNotFoundExceptionHandler {

    private final ApiResponseFactory responseFactory;

    public CommonNotFoundExceptionHandler(ApiResponseFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResponse<Object>> handleNotFound(
            Exception exception,
            HttpServletRequest request
    ) {
        return responseFactory.error(
                ApiStatusCode.NOT_FOUND,
                "Resource not found",
                exception.getClass().getSimpleName(),
                Map.of("path", request.getRequestURI()),
                request
        );
    }
}
