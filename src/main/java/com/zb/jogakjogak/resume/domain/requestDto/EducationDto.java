package com.zb.jogakjogak.resume.domain.requestDto;

import com.zb.jogakjogak.resume.entity.Education;
import com.zb.jogakjogak.resume.type.EducationLevel;
import com.zb.jogakjogak.resume.type.EducationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "학력 사항 DTO")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EducationDto {
    @Schema(description = "교육 수준", example = "BACHELOR", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "교육 수준을 입력해주세요.")
    private EducationLevel level;
    @Schema(description = "주요 학문 분야", example = "컴퓨터공학", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "주요 학문 분야를 입력해주세요.")
    @Size(max = 225, message = "주요 학문 분야의 최대 길이는 225자입니다.")
    private String majorField;
    @Schema(description = "교육 상태", example = "GRADUATED", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "교육 상태를 입력해주세요.")
    private EducationStatus status;

    public static EducationDto from(Education education){
        return EducationDto.builder()
                .level(education.getLevel())
                .majorField(education.getMajorField())
                .status(education.getStatus())
                .build();
    }
}
