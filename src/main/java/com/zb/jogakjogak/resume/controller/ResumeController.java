package com.zb.jogakjogak.resume.controller;

import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeCreateRequestDtoV2;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeCreateRequestDto;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeUpdateRequestDto;
import com.zb.jogakjogak.resume.domain.requestDto.ResumeUpdateRequestDtoV2;
import com.zb.jogakjogak.resume.domain.responseDto.ResumeGetResponseDto;
import com.zb.jogakjogak.resume.domain.responseDto.ResumeResponseDto;
import com.zb.jogakjogak.resume.service.ResumeService;
import com.zb.jogakjogak.security.dto.CustomOAuth2User;
import com.zb.jogakjogak.global.exception.ErrorResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "이력서 관리 API", description = "이력서 등록, 수정, 조회, 삭제 API")
@RestController
@RequiredArgsConstructor
@RequestMapping
public class ResumeController {

    private final ResumeService resumeService;

    /**
     * 이력서 등록을 위한 컨틀로러 메소드
     *
     * @param requestDto 이력서 이름, 이력서 내용
     * @return data(이력서 id, 이력서 이름, 이력서 내용), 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "이력서 등록", description = "분석할 사용자의 이력서를 등록합니다. 이미 등록된 이력서가 있으면 409를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이력서 등록 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 이력서를 가지고 있음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/resume")
    public ResponseEntity<HttpApiResponse<ResumeResponseDto>> register(
            @Valid @RequestBody ResumeCreateRequestDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                new HttpApiResponse<>(
                        resumeService.register(requestDto, customOAuth2User.getMember()),
                        "이력서 등록 완료",
                        HttpStatus.CREATED
                )
        );
    }

    /**
     * 이력서 수정을 위한 컨트롤러 메서드
     *
     * @param resumeId   수정하려는 이력서의 id
     * @param requestDto 수정할 이력서 이름, 수정할 이력서 내용
     * @return data(수정한 이력서 id, 수정된 이력서 이름, 수정된 이력서 내용), 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "이력서 수정", description = "사용자가 등록한 이력서를 수정합니다. 요청 바디에 보낸 필드만 부분 수정되며, 보내지 않은 필드는 기존 값이 유지됩니다. resume_id가 존재하지 않으면 404, 본인 소유가 아니면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이력서 수정 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이력서", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 이력서에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/resume/{resume_id}")
    public ResponseEntity<HttpApiResponse<ResumeResponseDto>> modify(
            @PathVariable("resume_id") Long resumeId,
            @Valid @RequestBody ResumeUpdateRequestDto requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(
                                resumeService.modify(resumeId, requestDto, customOAuth2User.getMember()),
                                "이력서 수정 완료",
                                HttpStatus.OK
                        )
                );
    }

    /**
     * 사용자가 작성한 이력서를 조회하는 컨트롤러 메서드
     *
     * @param resumeId 찾으려는 이력서의 id
     * @return 찾으려는 이력서의 data, 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "이력서 조회", description = "사용자가 등록한 이력서를 조회합니다. resumeId가 존재하지 않으면 404, 본인 소유가 아니면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이력서 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이력서", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 이력서에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/resume/{resumeId}")
    public ResponseEntity<HttpApiResponse<ResumeResponseDto>> get(
            @PathVariable Long resumeId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(
                                resumeService.get(resumeId, customOAuth2User.getMember()),
                                "이력서 조회 성공",
                                HttpStatus.OK
                        )
                );
    }

    @Operation(summary = "이력서 삭제", description = "사용자가 등록한 이력서를 삭제합니다. resumeId가 존재하지 않으면 404, 본인 소유가 아니면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "이력서 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 이력서", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 이력서에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/resume/{resumeId}")
    public ResponseEntity<Void> delete(
            @PathVariable Long resumeId,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        resumeService.delete(resumeId, customOAuth2User.getMember());
        return ResponseEntity.noContent().build();
    }

    /**
     * (v2)이력서 등록을 위한 컨트롤러 메소드
     *
     * @param requestDto 이력서 내용, 신입 유무, 경력 리스트, 학력 리스트, 스킬 리스트
     * @return data(이력서 id, 이력서 내용, 신입 유무, 경력 리스트, 학력 리스트, 스킬 리스트, 생성일시, 수정일시), 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "(v2) 이력서 등록", description = "분석할 사용자의 이력서를 신입 유무, 경력, 학력, 스킬 목록 형식으로 등록합니다. 이미 등록된 이력서가 있으면 409, 신입이 아닌데 경력이 비어있으면 400을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "이력서 등록 완료"),
            @ApiResponse(responseCode = "400", description = "신입이 아닌데 경력이 비어있음, 입력값 검증 실패 등", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "409", description = "이미 이력서를 가지고 있음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping("/v2/resume")
    public ResponseEntity<HttpApiResponse<ResumeGetResponseDto>> registerV2(
            @Valid @RequestBody ResumeCreateRequestDtoV2 requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new HttpApiResponse<>(
                        resumeService.registerV2(requestDto, customOAuth2User.getMember()),
                        "이력서 등록 완료",
                        HttpStatus.CREATED
                )
        );
    }

    /**
     * 사용자가 작성한 이력서를 조회하는 컨트롤러 메서드
     *
     * @return 찾으려는 이력서의 data, 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "(v2) 이력서 조회", description = "사용자가 등록한 이력서를 신입 유무, 경력, 학력, 스킬 목록 형식으로 조회합니다. 등록된 이력서가 없으면 404를 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이력서 조회 성공"),
            @ApiResponse(responseCode = "404", description = "등록된 이력서 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/v2/resume")
    public ResponseEntity<HttpApiResponse<ResumeGetResponseDto>> getResumeV2(
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(
                                resumeService.getResumeV2(customOAuth2User.getMember()),
                                "이력서 조회 성공",
                                HttpStatus.OK
                        )
                );
    }

    /**
     * 이력서 수정을 위한 컨트롤러 메서드
     *
     * @param requestDto 수정할 이력서 이름, 수정할 이력서 내용
     * @return data(수정한 이력서 id, 수정된 이력서 이름, 수정된 이력서 내용), 성공 여부 메세지, 상태코드
     */
    @Operation(summary = "(v2) 이력서 수정", description = "사용자가 등록한 이력서를 신입 유무, 경력, 학력, 스킬 목록 형식으로 수정합니다. 요청 바디에 보낸 필드만 부분 수정되며, career/education/skill 목록은 보낸 경우에만 해당 목록 전체가 통째로 교체되고 보내지 않으면 기존 목록이 유지됩니다. 등록된 이력서가 없으면 404, (수정 결과 기준으로) 신입이 아닌데 경력이 비어있으면 400을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "이력서 수정 완료"),
            @ApiResponse(responseCode = "404", description = "등록된 이력서 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "신입이 아닌데 경력이 비어있음, 입력값 검증 실패 등", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/v2/resume")
    public ResponseEntity<HttpApiResponse<ResumeGetResponseDto>> modifyV2(
            @Valid @RequestBody ResumeUpdateRequestDtoV2 requestDto,
            @AuthenticationPrincipal CustomOAuth2User customOAuth2User) {
        return ResponseEntity.ok()
                .body(
                        new HttpApiResponse<>(
                                resumeService.modifyV2(requestDto, customOAuth2User.getMember()),
                                "이력서 수정 완료",
                                HttpStatus.OK
                        )
                );
    }
}
