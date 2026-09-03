package com.zb.jogakjogak.security.jwt;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.security.Token;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JWTUtilTest {

    private final JWTUtil jwtUtil = new JWTUtil(new JwtConfig(
            "test-secret-key-must-be-at-least-32-bytes-long!!",
            "jogakjogak",
            "jogakjogak-client",
            1800000L,
            604800000L
    ));

    @Test
    void createAccessToken_토큰_발급_후_검증에_성공한다() {
        String access = jwtUtil.createAccessToken(1L, "kakao", "kakao 123", "USER", Token.ACCESS_TOKEN);

        Claims claims = jwtUtil.validateToken(access, Token.ACCESS_TOKEN);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getId()).isNull();
        assertThat(jwtUtil.getProvider(access)).isEqualTo("kakao");
        assertThat(jwtUtil.getUsername(claims)).isEqualTo("kakao 123");
        assertThat(jwtUtil.getRole(claims)).isEqualTo("USER");
    }

    @Test
    void createRefreshToken_토큰_발급_후_검증에_성공한다() {
        String refresh = jwtUtil.createRefreshToken(1L, Token.REFRESH_TOKEN);

        Claims claims = jwtUtil.validateToken(refresh, Token.REFRESH_TOKEN);

        assertThat(claims.getSubject()).isEqualTo("1");
        assertThat(claims.getId()).isNotNull();
        assertThat(jwtUtil.getJti(claims)).isEqualTo(claims.getId());
    }

    @Test
    void refresh_토큰을_access_자리에서_검증하면_예외() {
        String refresh = jwtUtil.createRefreshToken(1L, Token.REFRESH_TOKEN);

        assertThatThrownBy(() -> jwtUtil.validateToken(refresh, Token.ACCESS_TOKEN))
                .isInstanceOf(AuthException.class);
    }

    @Test
    void 다른_issuer_audience_설정으로_발급된_토큰은_검증에_실패한다() {
        JWTUtil otherIssuerUtil = new JWTUtil(new JwtConfig(
                "test-secret-key-must-be-at-least-32-bytes-long!!",
                "other-issuer",
                "jogakjogak-client",
                1800000L,
                604800000L
        ));
        String token = otherIssuerUtil.createAccessToken(1L, "kakao", "kakao 123", "USER", Token.ACCESS_TOKEN);

        assertThatThrownBy(() -> jwtUtil.validateToken(token, Token.ACCESS_TOKEN))
                .isInstanceOf(AuthException.class);
    }
}
