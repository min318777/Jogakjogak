package com.zb.jogakjogak.resume.domain.requestDto;

import com.zb.jogakjogak.global.validation.MeaningfulText;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Schema(description = "(v2) 이력서 수정 요청 DTO (보낸 필드만 부분 수정됩니다. careerList/educationList/skillList는 보낸 항목만 통째로 교체되고, 보내지 않으면 기존 내용이 유지됩니다)")
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeUpdateRequestDtoV2 {
    @Schema(description = "이력서 내용", example = "핵심 역량, 프로젝트 경험 등", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 5000, message = "이력서는 5000자 이내여야 합니다.")
    @MeaningfulText(message = "이력서 내용이 유효하지 않거나 의미 없는 반복 문자를 포함합니다.")
    private String content;
    @Schema(description = "신입 여부", example = "true", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private Boolean isNewcomer;
    @Valid
    private List<CareerDto> careerList;
    @Valid
    private List<EducationDto> educationList;
    private List<String> skillList;
}
