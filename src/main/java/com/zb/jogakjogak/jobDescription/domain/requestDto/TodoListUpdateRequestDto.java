package com.zb.jogakjogak.jobDescription.domain.requestDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.zb.jogakjogak.jobDescription.type.ToDoListType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "todolist 수정 요청 DTO (보낸 필드만 부분 수정됩니다)")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TodoListUpdateRequestDto {
    @Schema(description = "todolist 카테고리", example = "STRUCTURAL_COMPLEMENT_PLAN", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private ToDoListType category;
    @Schema(description = "todolist 제목", example = "NoSQL 학습", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 50, message = "제목의 최대 길이는 50자입니다.")
    private String title;
    @Schema(description = "todolist 내용", example = "유튜브에 있는 NoSQL에 관련된 강의 듣기", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private String content;
    @Schema(description = "todolist 완료 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @JsonProperty("is_done")
    private Boolean isDone;
}
