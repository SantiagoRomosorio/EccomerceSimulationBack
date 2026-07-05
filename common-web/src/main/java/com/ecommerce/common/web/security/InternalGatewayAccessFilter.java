package com.ecommerce.common.web.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

public class InternalGatewayAccessFilter extends OncePerRequestFilter {

    public static final String HEADER_NAME = "X-Internal-Gateway-Token";

    private final byte[] gatewayToken;

    public InternalGatewayAccessFilter(String gatewayToken) {
        if (!StringUtils.hasText(gatewayToken)) {
            throw new IllegalArgumentException("Internal gateway token must not be blank");
        }
        this.gatewayToken = gatewayToken.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = request.getHeader(HEADER_NAME);
        if (hasValidToken(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        writeNotFound(response, request);
    }

    private boolean hasValidToken(String token) {
        if (token == null) {
            return false;
        }
        return MessageDigest.isEqual(gatewayToken, token.getBytes(StandardCharsets.UTF_8));
    }

    private void writeNotFound(HttpServletResponse response, HttpServletRequest request) throws IOException {
        response.setStatus(HttpServletResponse.SC_NOT_FOUND);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType("application/json");
        response.getWriter().write("""
                {"timestamp":"%s","method":"%s","status":404,"result":"Not Found","developerMessage":"InternalGatewayAccessFilter","message":"Resource not found","path":"%s","data":null}
                """.formatted(Instant.now(), request.getMethod(), request.getRequestURI()).trim());
    }
}
