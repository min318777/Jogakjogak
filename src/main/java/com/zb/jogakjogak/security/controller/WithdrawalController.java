package com.zb.jogakjogak.security.controller;


import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.security.service.WithdrawalService;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.Null;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.graphql.GraphQlProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "회원 관리 API", description = "회원 정보 조회/수정, 알림 설정, 탈퇴 등 회원 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/member/withdrawal")
public class WithdrawalController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "회원 탈퇴", description = "로그인된 사용자가 회원을 탈퇴합니다. 인증되지 않았거나 구글 access token이 만료되었으면 401, 회원 또는 소셜 로그인 정보가 존재하지 않으면 404, 소셜 계정 연결 해제에 실패하면 417을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "회원탈퇴 완료"),
            @ApiResponse(responseCode = "401", description = "인증되지 않은 사용자, 또는 구글 access token 만료", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원 또는 소셜 로그인 정보", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "417", description = "카카오/구글 계정 연결 해제 실패로 회원탈퇴 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping
    public ResponseEntity<HttpApiResponse<?>> oauth2Withdrawal(HttpServletResponse response, @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new HttpApiResponse<>(null,
                            "회원 탈퇴 요청 실패: 인증되지 않은 사용자입니다.",
                            HttpStatus.UNAUTHORIZED));
        }
        withdrawalService.withdrawMember(customOAuth2User.getName());
        clearCookie(response);
        SecurityContextHolder.clearContext();
        return ResponseEntity.ok()
                .body(new HttpApiResponse<>(null,
                        "회원탈퇴 완료",
                        HttpStatus.OK));
    }

    private void clearCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refresh", null);
        cookie.setMaxAge(0);
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        response.addCookie(cookie);
    }
}
