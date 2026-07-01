package com.ecommerce.identity.adapter.port.out.jwt;

import com.ecommerce.identity.application.port.out.GenerateTokenPort;
import com.ecommerce.identity.config.properties.JwtProperties;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenAdapter implements GenerateTokenPort {

    private final JwtProperties jwtProperties;

    public JwtTokenAdapter(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
    }

    @Override
    public String generate(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(jwtProperties.expiration());

        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey())
                .compact();
    }

    private SecretKey secretKey() {
        return Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }
}
