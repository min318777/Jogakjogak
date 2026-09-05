package com.zb.jogakjogak.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.repository.MemberRepository;
import com.zb.jogakjogak.security.service.BlacklistService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JWTFilter extends OncePerRequestFilter {

    private final JWTUtil jwtUtil;
    private final BlacklistService blacklistService;
    private final MemberRepository memberRepository;
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

        Long userId = Long.parseLong(jwtUtil.getUserId(claims));
        String role = memberRepository.findById(userId)
                .map(member -> member.getRole().name())
                .orElse(null);
        if (role == null) {
            writeErrorResponse(response, "NOT_FOUND_MEMBER", "존재하지 않는 회원입니다.");
            return;
        }

        List<GrantedAuthority> authorities = List.of(new SimpleGrantedAuthority(role));
        Authentication authToken = new UsernamePasswordAuthenticationToken(userId, null, authorities);
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
