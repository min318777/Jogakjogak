package com.zb.jogakjogak.security.controller;


import com.zb.jogakjogak.global.CommonResponse;
import com.zb.jogakjogak.security.dto.ReissueResultDto;
import com.zb.jogakjogak.security.service.ReissueService;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "인증 관리 API", description = "로그인, 토큰 재발급 등 인증 관련 API")
@RestController
@RequiredArgsConstructor
public class ReissueController {

    private final ReissueService reissueService;

    @Operation(summary = "Access Token 재발급", description = "쿠키의 refresh token으로 새로운 access/refresh token을 재발급 받습니다. 토큰이 만료/탈취되었거나 유효하지 않으면 401, 토큰 또는 회원이 존재하지 않으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "access token 재발급 완료"),
            @ApiResponse(responseCode = "401", description = "만료/탈취되었거나 유효하지 않은 토큰", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 토큰 또는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/member/reissue")
    public ResponseEntity<CommonResponse<String>> reissue(HttpServletRequest request, HttpServletResponse response) {

        String refreshToken = extractRefreshTokenFromCookie(request.getCookies());
        ReissueResultDto reissueResultDto = reissueService.reissue(refreshToken);
        response.addHeader("Set-Cookie", createCookieHeader("refresh", reissueResultDto.getNewRefreshToken(), request));
        return ResponseEntity.ok()
                .body(
                        new CommonResponse<>(reissueResultDto.getNewAccessToken(),
                                "access token 재발급 완료"));
    }

    private String extractRefreshTokenFromCookie(Cookie[] cookies) {
            if (cookies == null) return null;
            for (Cookie cookie : cookies) {
                if ("refresh".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
            return null;
    }

    private String createCookieHeader(String key, String value, HttpServletRequest request) {
        boolean isLocal = request.getServerName().contains("localhost");
        if (isLocal) {
            return String.format(
                    "%s=%s; Max-Age=%d; Path=/; HttpOnly; SameSite=Lax",
                    key, value, 24 * 60 * 60 * 7
            );
        }
        return String.format(
                "%s=%s; Max-Age=%d; Path=/; Domain=.jogakjogak.com; HttpOnly; SameSite=Lax; Secure",
                key, value, 24 * 60 * 60 * 7
        );
    }
}
