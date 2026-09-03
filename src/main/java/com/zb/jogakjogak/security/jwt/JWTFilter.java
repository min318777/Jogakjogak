package com.zb.jogakjogak.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import com.zb.jogakjogak.security.Role;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.service.BlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final BlacklistService blacklistService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = extractAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        Claims claims;
        try {
            claims = jwtUtil.validateToken(accessToken, Token.ACCESS_TOKEN);
        } catch (AuthException e) {
            writeErrorResponse(response, e.getMemberErrorCode().name(), e.getMessage());
            return;
        }

        if (blacklistService.isBlacklisted(jwtUtil.getJti(claims))) {
            writeErrorResponse(response, "BLACKLISTED_TOKEN", "로그아웃된 토큰입니다.");
            return;
        }

        Member member = Member.builder()
                .id(Long.parseLong(jwtUtil.getUserId(claims)))
                .username(jwtUtil.getUsername(claims))
                .role(Role.valueOf(jwtUtil.getRole(claims)))
                .build();

        CustomOAuth2User customOAuth2User = new CustomOAuth2User(member);
        Authentication authToken = new UsernamePasswordAuthenticationToken(customOAuth2User, null, customOAuth2User.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }

    private void writeErrorResponse(HttpServletResponse response, String errorCode, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json; charset=UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse(errorCode, message)));
    }

    private String extractAccessToken(HttpServletRequest request){
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }
        return null;
    }
}
