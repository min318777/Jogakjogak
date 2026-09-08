package com.zb.jogakjogak.member.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.member.entity.OAuth2Info;
import com.zb.jogakjogak.member.repository.MemberRepository;
import com.zb.jogakjogak.security.jwt.JWTUtil;
import com.zb.jogakjogak.security.service.BlacklistService;
import com.zb.jogakjogak.security.service.RefreshTokenRedisService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class WithdrawalServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private RefreshTokenRedisService refreshTokenRedisService;

    @Mock
    private BlacklistService blacklistService;

    @Mock
    private JWTUtil jwtUtil;

    @Mock
    private KakaoWithdrawalService kakaoWithdrawalService;

    @Mock
    private GoogleWithdrawalService googleWithdrawalService;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("카카오 회원탈퇴 성공 - 카카오 연동 해제, 액세스 토큰 블랙리스트 등록 및 회원 삭제")
    void withdrawMember_kakao_success() {
        // given
        OAuth2Info kakaoInfo = OAuth2Info.builder()
                .provider("kakao")
                .providerId("kakao_12345")
                .build();

        Member member = Member.builder()
                .id(1L)
                .username("kakao_user")
                .oauth2Info(List.of(kakaoInfo))
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));
        given(jwtUtil.getJti("access-token")).willReturn("jti-1");
        given(jwtUtil.getExpiration("access-token")).willReturn(new Date(System.currentTimeMillis() + 60_000));

        // when
        withdrawalService.withdrawMember(1L, "access-token");

        // then
        then(kakaoWithdrawalService).should().unlinkKakaoMember("kakao_12345");
        then(googleWithdrawalService).should(never()).unlinkGoogleMember(any());
        then(refreshTokenRedisService).should().revokeAll(1L);
        then(blacklistService).should().addToBlacklist(eq("jti-1"), anyLong());
        then(memberRepository).should().delete(member);
    }

    @Test
    @DisplayName("구글 회원탈퇴 성공 - 구글 연동 해제, 액세스 토큰 블랙리스트 등록 및 회원 삭제")
    void withdrawMember_google_success() {
        // given
        OAuth2Info googleInfo = OAuth2Info.builder()
                .provider("google")
                .providerId("google_12345")
                .accessToken("google-access-token")
                .build();

        Member member = Member.builder()
                .id(2L)
                .username("google_user")
                .oauth2Info(List.of(googleInfo))
                .build();

        given(memberRepository.findById(2L)).willReturn(Optional.of(member));
        given(jwtUtil.getJti("access-token")).willReturn("jti-2");
        given(jwtUtil.getExpiration("access-token")).willReturn(new Date(System.currentTimeMillis() + 60_000));

        // when
        withdrawalService.withdrawMember(2L, "access-token");

        // then
        then(googleWithdrawalService).should().unlinkGoogleMember("google-access-token");
        then(kakaoWithdrawalService).should(never()).unlinkKakaoMember(any());
        then(refreshTokenRedisService).should().revokeAll(2L);
        then(blacklistService).should().addToBlacklist(eq("jti-2"), anyLong());
        then(memberRepository).should().delete(member);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 예외 발생")
    void withdrawMember_member_not_found() {
        // given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdrawMember(999L, "access-token"))
                .isInstanceOf(AuthException.class);

        then(memberRepository).should(never()).delete(any());
    }

    @Test
    @DisplayName("OAuth2Info가 없으면 예외 발생")
    void withdrawMember_oauth2Info_not_found() {
        // given
        Member member = Member.builder()
                .id(1L)
                .username("no_oauth_user")
                .oauth2Info(List.of())
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdrawMember(1L, "access-token"))
                .isInstanceOf(AuthException.class);

        then(memberRepository).should(never()).delete(any());
    }
}
