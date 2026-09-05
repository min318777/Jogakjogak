package com.zb.jogakjogak.security.config;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import com.zb.jogakjogak.security.jwt.CustomLogoutFilter;
import com.zb.jogakjogak.security.jwt.JWTFilter;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.oauth2.CustomSuccessHandler;
import com.zb.jogakjogak.security.repository.MemberRepository;
import com.zb.jogakjogak.security.service.BlacklistService;
import com.zb.jogakjogak.security.service.RefreshTokenRedisService;
import com.zb.jogakjogak.security.service.CustomOauth2UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.Arrays;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOauth2UserService customOauth2UserService;
    private final CustomSuccessHandler customSuccessHandler;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final JWTUtil jwtUtil;
    private final BlacklistService blacklistService;
    private final MemberRepository memberRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.
                cors(corsCustomizer -> corsCustomizer.configurationSource(new CorsConfigurationSource() {
                    @Override
                    public CorsConfiguration getCorsConfiguration(HttpServletRequest request) {
                        CorsConfiguration corsConfiguration = new CorsConfiguration();
                        corsConfiguration.setAllowedOrigins(Arrays.asList(
                                "http://localhost:3000",
                                "https://jogakjogak.com",
                                "https://jogakjogak-front.vercel.app",
                                "https://www.jogakjogak.com",
                                "https://api.jogakjogak.com"
                                ));
                        corsConfiguration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                        corsConfiguration.setAllowCredentials(true);
                        corsConfiguration.setAllowedHeaders(Arrays.asList(
                                "Authorization",
                                "Content-Type",
                                "X-Client-ID"
                        ));
                        corsConfiguration.setMaxAge(600L);
                        corsConfiguration.setExposedHeaders(Arrays.asList(
                                "Set-Cookie",
                                "Authorization",
                                "Access-Control-Allow-Credentials"
                        ));
                        return corsConfiguration;
                    }}));
        http.
                csrf((auth) -> auth.disable());
        http.
                formLogin((auth) -> auth.disable());
        http.
                httpBasic((auth) -> auth.disable());
        http.
                addFilterAfter(new JWTFilter(jwtUtil, blacklistService, memberRepository), OAuth2LoginAuthenticationFilter.class);
        http.
                addFilterBefore(new CustomLogoutFilter(refreshTokenRedisService, jwtUtil, blacklistService), LogoutFilter.class);
        http.
                oauth2Login((oauth2) -> oauth2
                        .userInfoEndpoint((userInfoEndpointConfig) -> userInfoEndpointConfig.userService(customOauth2UserService))
                        .successHandler(customSuccessHandler)
                        .failureHandler((request, response, exception) -> {
                            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                            response.setContentType("application/json; charset=UTF-8");
                            response.getWriter().write(objectMapper.writeValueAsString(new ErrorResponse("LOGIN_FAILED", "소셜 로그인에 실패했습니다.")));
                        })
                );
        http.
                authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/",
                                "/actuator/health",
                                "/oauth2/**",
                                "/login/oauth2/code/**",
                                "/member/reissue",
                                "/member/logout",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/swagger-resources/**",
                                "/webjars/**",
                                "/biz/init/**",
                                "/biz/send/**",
                                "/biz/batch/**"
                        ).permitAll()
                        .anyRequest().authenticated());
        http.
                sessionManagement((session) -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
        return http.build();
    }
}
