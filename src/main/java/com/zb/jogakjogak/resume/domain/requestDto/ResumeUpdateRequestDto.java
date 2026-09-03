package com.zb.jogakjogak.resume.domain.requestDto;

import com.zb.jogakjogak.global.validation.MeaningfulText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "이력서 수정 요청 DTO (보낸 필드만 부분 수정됩니다)")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUpdateRequestDto {
    @Schema(description = "이력서 제목", example = "6월 20일 이력서", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 30, message = "이력서 제목은 30자 이내여야 합니다.")
    private String title;
    @Schema(description = "이력서 내용", example = "핵심 역량, 프로젝트 경험 등", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(min = 300, max = 5000, message = "이력서는 300자 이상 5000자 이내여야 합니다.")
    @MeaningfulText(message = "이력서 내용이 유효하지 않거나 의미 없는 반복 문자를 포함합니다.")
    private String content;
}
