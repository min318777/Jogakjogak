package com.zb.jogakjogak.security.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.security.entity.Member;
import com.zb.jogakjogak.security.entity.OAuth2Info;
import com.zb.jogakjogak.security.repository.MemberRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
    private KakaoWithdrawalService kakaoWithdrawalService;

    @Mock
    private GoogleWithdrawalService googleWithdrawalService;

    @InjectMocks
    private WithdrawalService withdrawalService;

    @Test
    @DisplayName("카카오 회원탈퇴 성공 - 카카오 연동 해제 및 회원 삭제")
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

        given(memberRepository.findByUsername("kakao_user")).willReturn(Optional.of(member));

        // when
        withdrawalService.withdrawMember("kakao_user");

        // then
        then(kakaoWithdrawalService).should().unlinkKakaoMember("kakao_12345");
        then(googleWithdrawalService).should(never()).unlinkGoogleMember(any());
        then(refreshTokenRedisService).should().revokeAll(1L);
        then(memberRepository).should().delete(member);
    }

    @Test
    @DisplayName("구글 회원탈퇴 성공 - 구글 연동 해제 및 회원 삭제")
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

        given(memberRepository.findByUsername("google_user")).willReturn(Optional.of(member));

        // when
        withdrawalService.withdrawMember("google_user");

        // then
        then(googleWithdrawalService).should().unlinkGoogleMember("google-access-token");
        then(kakaoWithdrawalService).should(never()).unlinkKakaoMember(any());
        then(refreshTokenRedisService).should().revokeAll(2L);
        then(memberRepository).should().delete(member);
    }

    @Test
    @DisplayName("회원이 존재하지 않으면 예외 발생")
    void withdrawMember_member_not_found() {
        // given
        given(memberRepository.findByUsername("unknown")).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdrawMember("unknown"))
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

        given(memberRepository.findByUsername("no_oauth_user")).willReturn(Optional.of(member));

        // when & then
        assertThatThrownBy(() -> withdrawalService.withdrawMember("no_oauth_user"))
                .isInstanceOf(AuthException.class);

        then(memberRepository).should(never()).delete(any());
    }
}
