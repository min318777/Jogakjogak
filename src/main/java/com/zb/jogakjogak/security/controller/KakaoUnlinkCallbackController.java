package com.zb.jogakjogak.security.controller;

import com.zb.jogakjogak.security.service.WithdrawalService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "카카오 연결 끊기 Callback API", description = "카카오 계정 설정에서 앱 연결을 끊었을 때 카카오가 호출하는 콜백")
@RestController
@RequiredArgsConstructor
@RequestMapping("/oauth2/kakao")
public class KakaoUnlinkCallbackController {

    private final WithdrawalService withdrawalService;

    @Operation(summary = "카카오 연결 끊기 콜백", description = "카카오 계정 설정에서 서비스 연결을 끊으면 카카오가 user_id를 담아 호출합니다. 해당 user_id의 회원이 없으면 아무 동작 없이 200을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "콜백 처리 완료")
    })
    @PostMapping("/unlink-callback")
    public ResponseEntity<Void> unlinkCallback(@RequestParam("user_id") String userId) {
        withdrawalService.withdrawByKakaoCallback(userId);
        return ResponseEntity.ok().build();
    }
}
