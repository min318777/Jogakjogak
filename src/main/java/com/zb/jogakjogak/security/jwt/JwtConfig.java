package com.zb.jogakjogak.security.jwt;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public record JwtConfig(
        String secret,
        String issuer,
        String audience,
        long accessTtlMs,
        long refreshTtlMs
) {
    public JwtConfig(
            @Value("${jwt.secret-key}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.audience}") String audience,
            @Value("${jwt.access-ttl-ms}") long accessTtlMs,
            @Value("${jwt.refresh-ttl-ms}") long refreshTtlMs
    ) {
        this.secret = secret;
        this.issuer = issuer;
        this.audience = audience;
        this.accessTtlMs = accessTtlMs;
        this.refreshTtlMs = refreshTtlMs;
    }
}
