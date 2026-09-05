package com.zb.jogakjogak.security.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.ReissueResultDto;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.repository.MemberRepository;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReissueServiceTest {

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @Mock
    private MemberRepository memberRepository;

    @InjectMocks
    private ReissueService reissueService;

    private static final Long USER_ID = 1L;
    private static final String REFRESH_TOKEN = "refresh-token";
    private static final String REFRESH_TOKEN_JTI = "refresh-token-jti";
    private static final String NEW_ACCESS = "new-access-token";
    private static final String NEW_REFRESH = "new-refresh-token";
    private static final String NEW_REFRESH_JTI = "new-refresh-token-jti";

    @Test
    @DisplayName("정상 재발급 - 새 액세스/리프레시 토큰 반환")
    void reissue_success() {
        // given
        Claims claims = mock(Claims.class);

        given(jwtUtil.validateToken(REFRESH_TOKEN, Token.REFRESH_TOKEN)).willReturn(claims);
        given(jwtUtil.getUserId(claims)).willReturn(USER_ID.toString());
        given(jwtUtil.getJti(claims)).willReturn(REFRESH_TOKEN_JTI);
        given(refreshTokenRedisService.exists(USER_ID, REFRESH_TOKEN_JTI)).willReturn(true);
        given(memberRepository.existsById(USER_ID)).willReturn(true);
        given(jwtUtil.createAccessToken(eq(USER_ID), any(), eq(Token.ACCESS_TOKEN))).willReturn(NEW_ACCESS);
        given(jwtUtil.createRefreshToken(eq(USER_ID), eq(Token.REFRESH_TOKEN))).willReturn(NEW_REFRESH);
        given(jwtUtil.getJti(NEW_REFRESH)).willReturn(NEW_REFRESH_JTI);

        // when
        ReissueResultDto result = reissueService.reissue(REFRESH_TOKEN);

        // then
        assertThat(result.getNewAccessToken()).isEqualTo(NEW_ACCESS);
        assertThat(result.getNewRefreshToken()).isEqualTo(NEW_REFRESH);
        then(refreshTokenRedisService).should().revoke(USER_ID, REFRESH_TOKEN_JTI);
        then(refreshTokenRedisService).should().save(USER_ID, NEW_REFRESH_JTI);
    }

    @Test
    @DisplayName("Redis에 해당 jti 없으면 토큰 탈취 감지, 전체 세션 폐기 후 예외")
    void reissue_token_theft_detected() {
        // given
        Claims claims = mock(Claims.class);
        given(jwtUtil.validateToken(REFRESH_TOKEN, Token.REFRESH_TOKEN)).willReturn(claims);
        given(jwtUtil.getUserId(claims)).willReturn(USER_ID.toString());
        given(jwtUtil.getJti(claims)).willReturn(REFRESH_TOKEN_JTI);
        given(refreshTokenRedisService.exists(USER_ID, REFRESH_TOKEN_JTI)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class);

        then(refreshTokenRedisService).should().revokeAll(USER_ID);
    }

    @Test
    @DisplayName("Member 미존재 - NOT_FOUND_MEMBER 예외")
    void reissue_member_not_found() {
        // given
        Claims claims = mock(Claims.class);
        given(jwtUtil.validateToken(REFRESH_TOKEN, Token.REFRESH_TOKEN)).willReturn(claims);
        given(jwtUtil.getUserId(claims)).willReturn(USER_ID.toString());
        given(jwtUtil.getJti(claims)).willReturn(REFRESH_TOKEN_JTI);
        given(refreshTokenRedisService.exists(USER_ID, REFRESH_TOKEN_JTI)).willReturn(true);
        given(memberRepository.existsById(USER_ID)).willReturn(false);

        // when & then
        assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class);
    }
}
