package com.zb.jogakjogak.event.controller;

import com.zb.jogakjogak.event.domain.responseDto.EventResponseDto;
import com.zb.jogakjogak.event.service.EventService;
import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "이벤트 관리 API",
description = "사용자 이벤트 관련 API")
@RestController
@RequestMapping("/event")
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @Operation(summary = "이벤트 단건 조회 및 수정", description = "처음 새 이용자 이벤트를 조회하면 isFirst를 true로, 두번째부턴 false로 응답합니다. 이벤트 코드가 존재하지 않으면 400을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "새 이용자 이벤트 조회 성공"),
            @ApiResponse(responseCode = "400", description = "존재하지 않는 이벤트 코드", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping()
    public ResponseEntity<HttpApiResponse<EventResponseDto>> getNewMemberEvent(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User
    ){
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        eventService.getNewMemberEvent(customOAuth2User.getMember()),
                        "새 이용자 이벤트 조회 성공",
                        HttpStatus.OK
                )
        );
    }
}
