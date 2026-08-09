package com.zb.jogakjogak.security.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.security.Token;
import com.zb.jogakjogak.security.dto.ReissueResultDto;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.entity.OAuth2Info;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

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
    private static final String NEW_ACCESS = "new-access-token";
    private static final String NEW_REFRESH = "new-refresh-token";
    private static final String PROVIDER = "kakao";
    private static final String USERNAME = "testuser";

    @Test
    @DisplayName("정상 재발급 - 새 액세스/리프레시 토큰 반환")
    void reissue_success() {
        // given
        OAuth2Info oauth2Info = mock(OAuth2Info.class);
        Member member = mock(Member.class);

        given(jwtUtil.getUserId(REFRESH_TOKEN)).willReturn(USER_ID.toString());
        given(refreshTokenRedisService.get(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(memberRepository.findById(USER_ID)).willReturn(Optional.of(member));
        given(member.getOauth2Info()).willReturn(List.of(oauth2Info));
        given(oauth2Info.getProvider()).willReturn(PROVIDER);
        given(member.getUsername()).willReturn(USERNAME);
        given(jwtUtil.createAccessToken(eq(USER_ID), eq(PROVIDER), eq(USERNAME), any(), anyLong(), eq(Token.ACCESS_TOKEN))).willReturn(NEW_ACCESS);
        given(jwtUtil.createRefreshToken(eq(USER_ID), anyLong(), eq(Token.REFRESH_TOKEN))).willReturn(NEW_REFRESH);

        // when
        ReissueResultDto result = reissueService.reissue(REFRESH_TOKEN);

        // then
        assertThat(result.getNewAccessToken()).isEqualTo(NEW_ACCESS);
        assertThat(result.getNewRefreshToken()).isEqualTo(NEW_REFRESH);
        then(refreshTokenRedisService).should().save(USER_ID, NEW_REFRESH);
    }

    @Test
    @DisplayName("Redis에 토큰 없으면 NOT_FOUND_TOKEN 예외")
    void reissue_token_not_found_in_redis() {
        // given
        given(jwtUtil.getUserId(REFRESH_TOKEN)).willReturn(USER_ID.toString());
        given(refreshTokenRedisService.get(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class);
    }

    @Test
    @DisplayName("Redis 토큰과 불일치 - 토큰 탈취 감지, Redis 삭제 후 예외")
    void reissue_token_theft_detected() {
        // given
        given(jwtUtil.getUserId(REFRESH_TOKEN)).willReturn(USER_ID.toString());
        given(refreshTokenRedisService.get(USER_ID)).willReturn(Optional.of("different-token"));

        // when & then
        assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class);

        then(refreshTokenRedisService).should().delete(USER_ID);
    }

    @Test
    @DisplayName("Member 미존재 - NOT_FOUND_MEMBER 예외")
    void reissue_member_not_found() {
        // given
        given(jwtUtil.getUserId(REFRESH_TOKEN)).willReturn(USER_ID.toString());
        given(refreshTokenRedisService.get(USER_ID)).willReturn(Optional.of(REFRESH_TOKEN));
        given(memberRepository.findById(USER_ID)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> reissueService.reissue(REFRESH_TOKEN))
                .isInstanceOf(AuthException.class);
    }
}
