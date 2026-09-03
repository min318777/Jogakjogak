package com.zb.jogakjogak.jobDescription.domain.requestDto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "다중 Todolist 생성/수정 요청 DTO")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TodoListBulkItemDto {
    @Schema(description = "todolist 아이디", example = "1")
    @JsonProperty("checklist_id")
    private Long id;
    @Schema(description = "todolist 제목", example = "NoSQL 학습", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "제목은 필수 입력 항목입니다.")
    @Size(max = 50, message = "제목의 최대 길이는 50자입니다.")
    private String title;
    @Schema(description = "todolist 내용", example = "유튜브에 있는 NoSQL에 관련된 강의 듣기", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "상세 설명은 필수 입력 항목입니다.")
    private String content;
    @Schema(description = "todolist 완료 여부", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonProperty("done")
    private boolean isDone;
}
