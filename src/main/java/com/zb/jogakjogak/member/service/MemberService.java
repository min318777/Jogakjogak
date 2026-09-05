package com.zb.jogakjogak.member.service;

import com.zb.jogakjogak.global.exception.AuthException;
import com.zb.jogakjogak.global.exception.MemberErrorCode;
import com.zb.jogakjogak.member.config.NicknameCreator;
import com.zb.jogakjogak.member.dto.response.MemberResponseDto;
import com.zb.jogakjogak.member.dto.response.UpdateIsOnboardedResponseDto;
import com.zb.jogakjogak.member.dto.request.UpdateMemberRequestDto;
import com.zb.jogakjogak.member.entity.Member;
import com.zb.jogakjogak.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final NicknameCreator nicknameCreator;


    public MemberResponseDto getMember(Long userId){
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        return MemberResponseDto.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .isNotificationEnabled(member.isNotificationEnabled())
                .build();
    }

    @Transactional
    public MemberResponseDto updateMember(Long userId, UpdateMemberRequestDto updateMemberRequestDto){
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));
        String newNickname = updateMemberRequestDto.getNickname();
        if (newNickname != null && !newNickname.equals(member.getNickname())
                && memberRepository.existsByNickname(newNickname)) {
            throw new AuthException(MemberErrorCode.ALREADY_EXISTING_NICKNAME);
        }
        member.updateMember(updateMemberRequestDto.getNickname(), updateMemberRequestDto.getIsNotificationEnabled());
        return MemberResponseDto.builder()
                .nickname(member.getNickname())
                .email(member.getEmail())
                .isNotificationEnabled(member.isNotificationEnabled())
                .build();
    }

    @Transactional
    public UpdateIsOnboardedResponseDto updateIsOnboarded(Long userId) {
        Member member = memberRepository.findById(userId)
                .orElseThrow(() -> new AuthException(MemberErrorCode.NOT_FOUND_MEMBER));

        boolean toggleOnboarded = !member.isOnboarded();
        member.updateOnboarded(toggleOnboarded);
        return UpdateIsOnboardedResponseDto.builder()
                .isOnboarded(toggleOnboarded)
                .build();
    }
}
