package com.ecommerce.common.web.openapi;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalOpenApiAccessFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Docs-Token";

    private final String gatewayToken;

    public InternalOpenApiAccessFilter(String gatewayToken) {
        this.gatewayToken = gatewayToken;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        if (!isOpenApiRequest(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = request.getHeader(HEADER_NAME);
        if (gatewayToken.equals(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeNotFound(response, request);
    }

    private boolean isOpenApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.equals("/v3/api-docs")
                || path.startsWith("/v3/api-docs/")
                || path.equals("/swagger-ui.html")
                || path.startsWith("/swagger-ui/")
                || path.startsWith("/webjars/swagger-ui/");
    }

    private void writeNotFound(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setContentType("application/json");
        response.getWriter().write("""
                {"timestamp":"%s","method":"%s","status":404,"result":"Not Found","developerMessage":"InternalOpenApiAccessFilter","message":"Resource not found","path":"%s","data":null}
                """.formatted(Instant.now(), request.getMethod(), request.getRequestURI()).trim());
    }
}
