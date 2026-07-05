package com.ecommerce.common.web.error;

import com.ecommerce.common.web.response.ApiResult;
import com.ecommerce.common.web.response.ApiResultFactory;
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

    private final ApiResultFactory responseFactory;

    public CommonNotFoundExceptionHandler(ApiResultFactory responseFactory) {
        this.responseFactory = responseFactory;
    }

    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<ApiResult<Object>> handleNotFound(
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
