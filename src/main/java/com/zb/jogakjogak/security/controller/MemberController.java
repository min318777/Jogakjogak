package com.zb.jogakjogak.security.controller;


import com.zb.jogakjogak.global.CommonResponse;
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
    public ResponseEntity<CommonResponse<MemberResponseDto>> getMember(@AuthenticationPrincipal Long userId){

        MemberResponseDto memberResponseDto = memberService.getMember(userId);
        return ResponseEntity.ok()
                .body(
                        new CommonResponse<>(memberResponseDto,
                                "회원정보 조회 완료")
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
    public ResponseEntity<CommonResponse<MemberResponseDto>> updateMember(@AuthenticationPrincipal Long userId,
                                                                           @Valid @RequestBody UpdateMemberRequestDto updateMemberRequestDto){
        MemberResponseDto memberResponseDto = memberService.updateMember(userId, updateMemberRequestDto);
        return ResponseEntity.ok()
                .body(
                        new CommonResponse<>(memberResponseDto,
                                "회원정보 수정 완료")
                );
    }

    @Operation(summary =  "회원 is_onboarded 수정", description = "회원의 is_onboarded 상태를 반전(toggle)합니다. 회원이 존재하지 않으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원 is_onboarded 수정 완료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/update-is-onboarded")
    public ResponseEntity<CommonResponse<UpdateIsOnboardedResponseDto>> updateIsOnboarded(@AuthenticationPrincipal Long userId) {
        UpdateIsOnboardedResponseDto updateIsOnboardedResponseDto = memberService.updateIsOnboarded(userId);

        return ResponseEntity.ok()
                .body(
                        new CommonResponse<>(updateIsOnboardedResponseDto,
                                "회원 is_onboarded를 " + updateIsOnboardedResponseDto.isOnboarded() + "로 수정 완료")
                );
    }
}
