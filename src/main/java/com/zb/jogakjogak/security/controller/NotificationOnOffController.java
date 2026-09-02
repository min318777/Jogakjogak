package com.zb.jogakjogak.security.controller;


import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.security.service.NotificationOnOffService;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@Tag(name = "회원 관리 API", description = "회원 정보 조회/수정, 알림 설정, 탈퇴 등 회원 관련 API")
@RestController
@RequestMapping("/member/notification/on-off")
@RequiredArgsConstructor
public class NotificationOnOffController {

    private final NotificationOnOffService notificationOnOffService;

    @Operation(summary = "이메일 전체 알림 수정", description = "회원의 모든 채용공고 알림을 일괄적으로 반전(toggle)합니다. 회원이 존재하지 않으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "전체 이메일 알림기능 수정 완료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 회원", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<HttpApiResponse<Boolean>> switchAllJdsNotification(@AuthenticationPrincipal CustomOAuth2User customOAuth2User){

        String username = customOAuth2User.getName();
        boolean notificationOnOff = notificationOnOffService.switchAllJdsNotification(username);
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(notificationOnOff,
                                "회원의 전체 이메일 알림기능 " + notificationOnOff + "로 수정 완료",
                                HttpStatus.OK)
                );
    }
}
