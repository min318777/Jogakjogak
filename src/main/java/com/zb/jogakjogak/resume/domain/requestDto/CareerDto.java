package com.zb.jogakjogak.resume.domain.requestDto;

import com.zb.jogakjogak.resume.entity.Career;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Schema(description = "경력 사항 DTO")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CareerDto {
    @Schema(description = "입사년월", example = "2023-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "입사년월을 입력해주세요.")
    private LocalDate joinedAt;
    @Schema(description = "퇴사년월", example = "2024-12-31", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDate quitAt;
    @Schema(description = "재직 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "재직 여부를 입력해주세요.")
    private Boolean isWorking;
    @Schema(description = "회사 이름", example = "조각조각", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "회사 이름을 입력해주세요.")
    @Size(max = 100, message = "회사 이름의 최대 길이는 100자입니다.")
    private String companyName;
    @Schema(description = "담당 업무와 주요 성과", example = "백엔드 API 설계 및 개발", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "담당 업무와 주요 성과를 입력해주세요.")
    @Size(max = 2000, message = "담당 업무와 주요 성과의 최대 길이는 2000자입니다.")
    private String workPerformance;

    public static CareerDto from(Career career) {
        return CareerDto.builder()
                .joinedAt(career.getJoinedAt())
                .quitAt(career.getQuitAt())
                .isWorking(career.getIsWorking())
                .companyName(career.getCompanyName())
                .workPerformance(career.getWorkPerformance())
                .build();
    }
}
