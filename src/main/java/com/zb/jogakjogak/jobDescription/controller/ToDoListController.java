package com.zb.jogakjogak.jobDescription.controller;

import com.zb.jogakjogak.global.HttpApiResponse;
import com.zb.jogakjogak.jobDescription.domain.requestDto.*;
import com.zb.jogakjogak.jobDescription.domain.responseDto.ToDoListGetByCategoryResponseDto;
import com.zb.jogakjogak.jobDescription.domain.responseDto.ToDoListResponseDto;
import com.zb.jogakjogak.jobDescription.domain.responseDto.UpdateIsDoneTodoListsResponseDto;
import com.zb.jogakjogak.jobDescription.service.ToDoListService;
import com.zb.jogakjogak.jobDescription.type.ToDoListType;
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

@Tag(name = "Todolist 관리 API", description = "JD/이력서 분석으로 생성된 Todolist 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/jds/{jd_id}/to-do-lists")
public class ToDoListController {

    private final ToDoListService toDoListService;

    /**
     * 특정 JD에 새로운 ToDoList를 생성합니다.
     *
     */
    @Operation(summary = "특정 분석/카테고리의 Todolist 생성", description = "jd_id와 category를 통해 todolist를 생성합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니면 403, 허용되지 않는 카테고리거나 해당 카테고리에 10개를 초과하면 400을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "체크리스트 추가 완료"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "허용되지 않는 카테고리, 카테고리별 개수 제한(10개) 초과, 또는 입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PostMapping
    public ResponseEntity<HttpApiResponse<ToDoListResponseDto>> createToDoList(
            @PathVariable("jd_id") Long jdId,
            @RequestBody @Valid TodoListCreateRequestDto dto,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        ToDoListResponseDto response = toDoListService.createToDoList(jdId, dto, customUser.getMember());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                new HttpApiResponse<>(
                        response,
                        "체크리스트 추가 완료",
                        HttpStatus.CREATED
                )
        );
    }

    /**
     * 특정 JD에 속한 기존 ToDoList의 내용을 수정합니다.
     */
    @Operation(summary = "특정 분석/카테고리의 Todolist 수정", description = "jd_id와 toDoList_id를 통해 todolist를 수정합니다. 요청 바디에 보낸 필드만 부분 수정되며, 보내지 않은 필드는 기존 값이 유지됩니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니거나 toDoListId가 해당 jd에 속하지 않으면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크리스트 수정 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한이 없거나, toDoListId가 해당 JD에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{toDoListId}")
    public ResponseEntity<HttpApiResponse<ToDoListResponseDto>> updateToDoList(
            @PathVariable("jd_id") Long jdId,
            @PathVariable Long toDoListId,
            @RequestBody @Valid TodoListUpdateRequestDto toDoListDto,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        ToDoListResponseDto response = toDoListService.updateToDoList(jdId, toDoListId, toDoListDto, customUser.getMember());
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        response,
                        "체크리스트 수정 완료",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "특정 분석/카테고리의 Todolist 완료 여부 수정", description = "jd_id와 toDoList_id를 통해 todolist 완료 여부를 수정합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니거나 toDoListId가 해당 jd에 속하지 않으면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크리스트 완료 여부 수정 완료"),
            @ApiResponse(responseCode = "400", description = "입력값 검증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한이 없거나, toDoListId가 해당 JD에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PatchMapping("/{toDoListId}/isDone")
    public ResponseEntity<HttpApiResponse<ToDoListResponseDto>> toggleComplete(
            @PathVariable("jd_id") Long jdId,
            @PathVariable Long toDoListId,
            @RequestBody @Valid TodoListIsDoneUpdateRequestDto toggleTodolist,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        ToDoListResponseDto response = toDoListService.toggleComplete(jdId, toDoListId, toggleTodolist, customUser.getMember());
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        response,
                        "체크리스트 완료 여부 수정 완료",
                        HttpStatus.OK
                )
        );
    }

    /**
     * 특정 JD에 속한 단일 ToDoList의 상세 정보를 조회합니다.

     */
    @Operation(summary = "특정 분석/카테고리의 Todolist 조회", description = "jd_id와 toDoList_id를 통해 todolist를 조회합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니거나 toDoListId가 해당 jd에 속하지 않으면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "체크리스트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한이 없거나, toDoListId가 해당 JD에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping("/{toDoListId}")
    public ResponseEntity<HttpApiResponse<ToDoListResponseDto>> getToDoList(
            @PathVariable("jd_id") Long jdId,
            @PathVariable Long toDoListId,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        toDoListService.getToDoList(jdId, toDoListId, customUser.getMember()),
                        "체크리스트 조회 성공",
                        HttpStatus.OK
                )
        );
    }

    /**
     * 특정 JD에 속한 단일 ToDoList를 삭제합니다.
     *
     */
    @Operation(summary = "특정 분석/카테고리의 Todolist 삭제", description = "jd_id와 toDoList_id를 통해 todolist를 삭제합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니거나 toDoListId가 해당 jd에 속하지 않으면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "체크리스트 삭제 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한이 없거나, toDoListId가 해당 JD에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @DeleteMapping("/{toDoListId}")
    public ResponseEntity<Void> deleteToDoList(
            @PathVariable("jd_id") Long jdId,
            @PathVariable Long toDoListId,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        toDoListService.deleteToDoList(jdId, toDoListId, customUser.getMember());
        return ResponseEntity.noContent().build();
    }

    /**
     * 특정 JD에 속한 특정 카테고리의 모든 ToDoList들을 조회합니다.
     *
     */
    @Operation(summary = "특정 분석/카테고리의 모든 Todolist 조회", description = "jd_id와 category를 통해 해당되는 모든 todolist를 조회합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "카테고리별 투두리스트 조회 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @GetMapping
    public ResponseEntity<HttpApiResponse<ToDoListGetByCategoryResponseDto>> getToDoListsByCategory(
            @PathVariable("jd_id")  Long jdId,
            @RequestParam(name = "category") ToDoListType category,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        toDoListService.getToDoListsByJdAndCategory(jdId, category, customUser.getMember()),
                        "카테고리별 투두리스트 조회 성공",
                        HttpStatus.OK
                )
        );
    }

    /**
     * 특정 JD에 속한 여러 ToDoList를 일괄적으로 생성, 수정, 삭제합니다.
     * 이 엔드포인트를 통해 복수 개의 ToDoList를 동시에 관리할 수 있습니다.
     */
    @Operation(summary = "특정 분석/카테고리의 모든 Todolist 생성/수정/삭제", description = "jd_id와 category를 통해 생성, 수정, 삭제된 todolist 정보를 리스트 형식으로 받아 업데이트합니다. jd_id가 존재하지 않으면 404, category가 없거나 허용되지 않거나 개수 제한(10개)을 초과하거나 수정/삭제 대상 toDoList가 해당 jd/카테고리에 속하지 않으면 400, 본인 소유가 아니거나 수정 대상 toDoList가 해당 jd에 속하지 않으면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다중 투두리스트 수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "400", description = "카테고리 누락/미허용, 개수 제한(10개) 초과, 또는 대상 투두리스트가 해당 JD/카테고리에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한이 없거나, 대상 투두리스트가 해당 JD에 속하지 않음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/bulk-update")
    public ResponseEntity<HttpApiResponse<ToDoListGetByCategoryResponseDto>> bulkUpdateToDoLists(
            @PathVariable("jd_id")  Long jdId,
            @RequestBody TodoListBulkUpdateRequestDto dto,
            @AuthenticationPrincipal CustomOAuth2User customUser) {
        toDoListService.bulkUpdateToDoLists(jdId, dto, customUser.getMember());
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        toDoListService.getToDoListsByJdAndCategory(jdId, dto.getCategory(), customUser.getMember()),
                        "다중 투두리스트 수정 성공",
                        HttpStatus.OK
                )
        );
    }

    @Operation(summary = "여러 Todolist의 완료여부 일괄 수정", description = "jd_id를 통해 여러 Todolist의 완료여부를 일괄적으로 수정합니다. jd_id가 존재하지 않으면 404, 본인 소유가 아니면 403을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "다중 투두리스트 완료여부 수정 성공"),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 JD", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "403", description = "해당 JD에 대한 권한 없음", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    @PutMapping("/update-is-done")
    public ResponseEntity<HttpApiResponse<UpdateIsDoneTodoListsResponseDto>> updateIsDoneTodoLists(
            @PathVariable("jd_id")  Long jdId,
            @RequestBody TodoListIsDoneBulkUpdateRequestDto dto,
            @AuthenticationPrincipal CustomOAuth2User customUser){
        return ResponseEntity.ok().body(
                new HttpApiResponse<>(
                        toDoListService.updateIsDoneTodoLists(jdId, dto, customUser.getMember()),
                        "다중 투두리스트 완료여부 수정 성공",
                        HttpStatus.OK
                )
        );
    }
}
