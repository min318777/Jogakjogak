package com.zb.jogakjogak.security.jwt;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.security.Token;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JWTUtil {

    private static final List<String> REQUIRED_CLAIMS = List.of("exp", "iat", "sub", "typ");

    private final JwtConfig jwtConfig;
    private final SecretKey secretKey;
    private final JwtParser parser;

    public JWTUtil(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
        this.secretKey = new SecretKeySpec(jwtConfig.secret().getBytes(StandardCharsets.UTF_8), Jwts.SIG.HS256.key().build().getAlgorithm());
        this.parser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtConfig.issuer())
                .requireAudience(jwtConfig.audience())
                .build();
    }

    private Claims parseClaims(String token) {
        Claims claims = parser.parseSignedClaims(token).getPayload();
        for (String claim : REQUIRED_CLAIMS) {
            if (claims.get(claim) == null) {
                throw new JwtException("Missing required claim: " + claim);
            }
        }
        return claims;
    }

    public String getUserId(String token){
        return getUserId(parseClaims(token));
    }

    public String getUserId(Claims claims){
        return claims.getSubject();
    }

    public String getProvider(String token){
        return parseClaims(token).get("provider", String.class);
    }

    public String getUsername(String token) {
        return getUsername(parseClaims(token));
    }

    public String getUsername(Claims claims) {
        return claims.get("username", String.class);
    }

    public String getRole(String token) {
        return getRole(parseClaims(token));
    }

    public String getRole(Claims claims) {
        return claims.get("role", String.class);
    }

    public Date getExpiration(String token) {
        return parseClaims(token).getExpiration();
    }

    public Date getExpiration(Claims claims) {
        return claims.getExpiration();
    }

    public String getJti(String token) {
        return getJti(parseClaims(token));
    }

    public String getJti(Claims claims) {
        return claims.getId();
    }

    public String createAccessToken(Long userId, String provider, String username, String role, Token token){
        return issue(token, userId, provider, username, role);
    }

    public String createRefreshToken(Long userId, Token token){
        return issue(token, userId, null, null, null);
    }

    private String issue(Token token, Long userId, String provider, String username, String role){
        var claims = Jwts.claims()
                .subject(String.valueOf(userId))
                .issuer(jwtConfig.issuer())
                .audience().add(jwtConfig.audience()).and()
                .add("typ", token.name());

        long expireMs;
        if (token == Token.ACCESS_TOKEN) {
            claims.add("provider", provider)
                    .add("username", username)
                    .add("role", role);
            expireMs = jwtConfig.accessTtlMs();
        } else {
            claims.id(UUID.randomUUID().toString());
            expireMs = jwtConfig.refreshTtlMs();
        }

        return Jwts.builder()
                .claims(claims.build())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expireMs))
                .signWith(secretKey)
                .compact();
    }

    public Claims validateToken(String token, Token tokenType) {
        if (token == null) {
            throw new AuthException(MemberErrorCode.NOT_FOUND_TOKEN);
        }
        try {
            Claims claims = parseClaims(token);

            if (claims.getExpiration().before(new Date())) {
                throw new AuthException(MemberErrorCode.TOKEN_EXPIRED);
            }

            if (!claims.get("typ", String.class).equals(tokenType.name())) {
                throw new AuthException(MemberErrorCode.TOKEN_TYPE_NOT_MATCH);
            }

            return claims;
        } catch (JwtException | IllegalArgumentException e){
            throw new AuthException(MemberErrorCode.INVALID_ACCESS_TOKEN);
        }
    }
}
