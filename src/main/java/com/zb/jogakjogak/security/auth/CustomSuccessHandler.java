package com.zb.jogakjogak.security.auth;

import com.zb.jogakjogak.ga.service.GaMeasurementProtocolService;
import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.member.repository.MemberRepository;
import com.zb.jogakjogak.security.service.RefreshTokenRedisService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CustomSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final JWTUtil jwtUtil;
    private final RefreshTokenRedisService refreshTokenRedisService;
    private final GaMeasurementProtocolService gaService;
    private final MemberRepository memberRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication
    authentication) throws IOException, ServletException {

        CustomOAuth2User customOAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String username = customOAuth2User.getName();
        Member member = memberRepository.findByUsername(username)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        Long userId = member.getId();
        String refreshToken = jwtUtil.createRefreshToken(userId);

        refreshTokenRedisService.save(userId, jwtUtil.getJti(refreshToken));
        addSameSiteCookieAttribute(request, response, "refresh", refreshToken);

        String clientId = extractGaClientId(request);
        String gaUserId = member.getId().toString();
        String eventName = "user_login";

        Map<String, Object> eventParams = new HashMap<>();
        eventParams.put("member_id", member.getId());
        eventParams.put("user_role", member.getRole());
        gaService.sendGaEvent(clientId, gaUserId, eventName, eventParams)
                .subscribe();

        String provider = ((OAuth2AuthenticationToken) authentication).getAuthorizedClientRegistrationId();
        String redirectUrl;
        if (request.getServerName().contains("localhost")) {
            redirectUrl = "http://localhost:3000/login/oauth2/code/" + provider;
        } else {
            redirectUrl = "https://www.jogakjogak.com/login/oauth2/code/" + provider;
        }

        response.sendRedirect(redirectUrl);
    }

    private void addSameSiteCookieAttribute(HttpServletRequest request, HttpServletResponse response, String
    cookieName, String cookieValue) {
        String serverName = request.getServerName();
        boolean isLocal = serverName.contains("localhost");

        String cookieHeader;
        if (isLocal) {
            // 로컬 환경
            cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                cookieName,
                cookieValue,
                60 * 60 * 24 * 7
            );
        } else {
            // 프로덕션 환경
            cookieHeader = String.format(
                "%s=%s; Max-Age=%d; Path=/; Domain=.jogakjogak.com; HttpOnly; SameSite=Lax; Secure",
                cookieName,
                cookieValue,
                60 * 60 * 24 * 7
            );
        }

        response.addHeader("Set-Cookie", cookieHeader);
    }


    private String extractGaClientId(HttpServletRequest request) {
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if ("_ga".equals(cookie.getName())) {
                    String gaCookieValue = cookie.getValue();
                    String[] parts = gaCookieValue.split("\\.");
                    if (parts.length >= 4) {
                        return parts[2] + "." + parts[3];
                    }
                }
            }
        }
        return null;
    }
}
