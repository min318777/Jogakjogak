package com.zb.jogakjogak.security.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.repository.MemberRepository;
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
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        String accessToken = extractAccessToken(request);

        if (accessToken == null) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            jwtUtil.validateToken(accessToken, Token.ACCESS_TOKEN);
        } catch (AuthException e) {
            writeErrorResponse(response, "UNAUTHORIZED", e.getMessage());
            return;
        }

        String userName = jwtUtil.getUsername(accessToken);
        Member member = memberRepository.findByUsername(userName)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

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
