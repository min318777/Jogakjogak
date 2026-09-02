package com.zb.jogakjogak.security.controller;


import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.security.dto.MemberResponseDto;
import com.zb.jogakjogak.security.dto.UpdateIsOnboardedResponseDto;
import com.zb.jogakjogak.security.dto.UpdateMemberRequestDto;
import com.zb.jogakjogak.security.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.checkerframework.checker.units.qual.C;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;


@Tag(name = "회원 관리 API", description = "회원 정보 조회/수정, 알림 설정, 탈퇴 등 회원 관련 API")
@RequestMapping("/member/my-page")
@RequiredArgsConstructor
@RestController
public class MemberController {

    private final MemberService memberService;

    @Operation(summary = "회원 상세정보 조회", description = "로그인된 회원의 정보를 조회합니다. 회원이 존재하지 않으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원정보 조회 완료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<HttpApiResponse<MemberResponseDto>> getMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){

        String username = customOAuth2User.getName();

        MemberResponseDto memberResponseDto = memberService.getMember(username);
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(memberResponseDto,
                                "회원정보 조회 완료",
                                HttpStatus.OK)
                );
    }

    @Operation(summary = "회원 상세정보 수정", description = "로그인된 회원의 정보를 수정합니다. 회원이 존재하지 않으면 404, 이미 사용 중인 닉네임이면 409를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원정보 수정 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 닉네임", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/update")
    public ResponseEntity<HttpApiResponse<MemberResponseDto>> updateMember(@AuthenticationPrincipal CustomOAuth2User customOAuth2User,
                                                                           @Valid @RequestBody UpdateMemberRequestDto updateMemberRequestDto){
        String username = customOAuth2User.getName();

        MemberResponseDto memberResponseDto = memberService.updateMember(username, updateMemberRequestDto);
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(memberResponseDto,
                                "회원정보 수정 완료",
                                HttpStatus.OK)
                );
    }

    @Operation(summary =  "회원 is_onboarded 수정", description = "회원의 is_onboarded 상태를 반전(toggle)합니다. 회원이 존재하지 않으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 is_onboarded 수정 완료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/update-is-onboarded")
    public ResponseEntity<HttpApiResponse<UpdateIsOnboardedResponseDto>> updateIsOnboarded(@AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        String username = customOAuth2User.getName();
        UpdateIsOnboardedResponseDto updateIsOnboardedResponseDto = memberService.updateIsOnboarded(username);

        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(updateIsOnboardedResponseDto,
                                "회원 is_onboarded를 " + updateIsOnboardedResponseDto.isOnboarded() + "로 수정 완료",
                                HttpStatus.OK)
                );
    }
}
