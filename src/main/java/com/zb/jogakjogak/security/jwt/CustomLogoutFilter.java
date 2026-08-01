package com.zb.jogakjogak.security.jwt;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.service.BlacklistService;
import com.zb.jogakjogak.security.service.RefreshTokenRedisService;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Date;

@Slf4j
@RequiredArgsConstructor
public class CustomLogoutFilter extends OncePerRequestFilter {
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final JWTUtil jwtUtil;
    private final BlacklistService blacklistService;
    private static final String LOGOUT_URI = "/member/logout";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        if (!isLogoutRequest(request)){
            filterChain.doFilter(request, response);
            return;
        }

        String refreshToken = extractRefreshTokenFromCookie(request.getCookies());

        if (refreshToken == null) {
            log.warn("[LogoutFilter] Refresh token 쿠키가 없음");
            clearRefreshTokenInCookie(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }

        try {
            jwtUtil.validateToken(refreshToken, Token.REFRESH_TOKEN);
        } catch (AuthException | JwtException | IllegalArgumentException e) {
            log.warn("[LogoutFilter] Refresh token 검증 실패 또는 username 추출 실패, 로그아웃 처리: {}", e.getMessage());
            clearRefreshTokenInCookie(response);
            response.setStatus(HttpServletResponse.SC_OK);
            return;
        }
        Long userId = Long.parseLong(jwtUtil.getUserId(refreshToken));
        refreshTokenRedisService.delete(userId);
        log.info("[LogoutFilter] {} 사용자 로그아웃 처리 (refresh token 삭제)", userId);

        // AccessToken 블랙리스트 등록
        String accessToken = extractAccessToken(request);
        if (accessToken != null) {
            try {
                String jti = jwtUtil.getJti(accessToken);
                Date expiration = jwtUtil.getExpiration(accessToken);
                long remainingMs = expiration.getTime() - System.currentTimeMillis();
                blacklistService.addToBlacklist(jti, remainingMs);
                log.info("[LogoutFilter] AccessToken 블랙리스트 등록 (jti={}, 남은 TTL: {}ms)", jti, remainingMs);
            } catch (Exception e) {
                log.warn("[LogoutFilter] AccessToken 블랙리스트 등록 실패 (무시): {}", e.getMessage());
            }
        }

        clearRefreshTokenInCookie(response);
        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\": \"로그아웃 완료\", \"code\": 200}");
    }

    private boolean isLogoutRequest(HttpServletRequest request) {
        return LOGOUT_URI.equals(request.getRequestURI()) && "POST".equals(request.getMethod());
    }

    private String extractAccessToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }

    private String extractRefreshTokenFromCookie(Cookie[] cookies) {
        if (cookies == null) return null;
        for (Cookie cookie : cookies) {
            if ("refresh".equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private void clearRefreshTokenInCookie(HttpServletResponse response){
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}