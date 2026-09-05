package com.zb.jogakjogak.member.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.member.config.NicknameCreator;
import com.zb.jogakjogak.member.dto.response.MemberResponseDto;
import com.zb.jogakjogak.member.dto.response.UpdateIsOnboardedResponseDto;
import com.zb.jogakjogak.member.dto.request.UpdateMemberRequestDto;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private NicknameCreator nicknameCreator;

    @InjectMocks
    private MemberService memberService;

    private Member testMember;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .id(1L)
                .username("testUser")
                .email("test@test.com")
                .nickname("oldNickname")
                .isNotificationEnabled(true)
                .build();
    }

    @Test
    @DisplayName("my-page - 회원이 존재하지 않으면 예외 발생")
    void getMember_notFound_test() {
        // given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.getMember(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(MemberErrorCode.NOT_FOUND_MEMBER.getMessage());
    }

    @Test
    @DisplayName("my-page - 닉네임 중복 없을 때 업데이트 성공")
    void updateMember_success_test() {
        // given
        UpdateMemberRequestDto dto = UpdateMemberRequestDto.builder()
                .nickname("newNickname")
                .isNotificationEnabled(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(memberRepository.existsByNickname("newNickname")).willReturn(false);

        // when
        MemberResponseDto result = memberService.updateMember(1L, dto);

        // then
        assertThat(result.getNickname()).isEqualTo("newNickname");
        assertThat(result.isNotificationEnabled()).isFalse();
    }

    @Test
    @DisplayName("my-page - 닉네임 중복일 경우 예외 발생")
    void updateMember_duplicateNickname_test() {
        // given
        UpdateMemberRequestDto dto = UpdateMemberRequestDto.builder()
                .nickname("duplicateNickname")
                .isNotificationEnabled(true)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
        given(memberRepository.existsByNickname("duplicateNickname")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(1L, dto))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(MemberErrorCode.ALREADY_EXISTING_NICKNAME.getMessage());
    }

    @Test
    @DisplayName("my-page - 회원이 존재하지 않으면 업데이트 시 예외 발생")
    void updateMember_memberNotFound_test() {
        // given
        UpdateMemberRequestDto dto = UpdateMemberRequestDto.builder()
                .nickname("newNickname")
                .build();
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.updateMember(999L, dto))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(MemberErrorCode.NOT_FOUND_MEMBER.getMessage());
    }

    @ParameterizedTest(name = "nickname={0}이면 existsByNickname 중복 체크를 하지 않는다")
    @DisplayName("my-page - 닉네임 미변경/미제공 시 중복 체크 없이 업데이트 성공")
    @NullSource
    @ValueSource(strings = "oldNickname")
    void updateMember_skipsDuplicateCheck_whenNicknameUnchangedOrAbsent(String nickname) {
        // given
        UpdateMemberRequestDto dto = UpdateMemberRequestDto.builder()
                .nickname(nickname)
                .isNotificationEnabled(false)
                .build();

        given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));

        // when
        MemberResponseDto result = memberService.updateMember(1L, dto);

        // then
        assertThat(result.getNickname()).isEqualTo("oldNickname");
        assertThat(result.isNotificationEnabled()).isFalse();
        verify(memberRepository, never()).existsByNickname(any());
    }

    @Test
    @DisplayName("updateIsOnboarded - false 상태를 true로 토글")
    void updateIsOnboarded_false_to_true_test() {
        // given
        Member member = Member.builder()
                .id(1L)
                .username("testUser")
                .isOnboarded(false)
                .build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        UpdateIsOnboardedResponseDto result = memberService.updateIsOnboarded(1L);

        // then
        assertThat(result.isOnboarded()).isTrue();
    }

    @Test
    @DisplayName("updateIsOnboarded - true 상태를 false로 토글")
    void updateIsOnboarded_true_to_false_test() {
        // given
        Member member = Member.builder()
                .id(1L)
                .username("testUser")
                .isOnboarded(true)
                .build();
        given(memberRepository.findById(1L)).willReturn(Optional.of(member));

        // when
        UpdateIsOnboardedResponseDto result = memberService.updateIsOnboarded(1L);

        // then
        assertThat(result.isOnboarded()).isFalse();
    }

    @Test
    @DisplayName("updateIsOnboarded - 회원이 존재하지 않으면 예외 발생")
    void updateIsOnboarded_memberNotFound_test() {
        // given
        given(memberRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> memberService.updateIsOnboarded(999L))
                .isInstanceOf(AuthException.class)
                .hasMessageContaining(MemberErrorCode.NOT_FOUND_MEMBER.getMessage());
    }
}
