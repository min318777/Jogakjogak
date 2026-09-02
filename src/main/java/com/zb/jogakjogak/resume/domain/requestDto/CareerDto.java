package com.zb.jogakjogak.resume.domain.requestDto;

import com.zb.jogakjogak.resume.entity.Career;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CareerDto {
    @NotNull(message = "입사년월을 입력해주세요.")
    private LocalDate joinedAt;
    private LocalDate quitAt;
    @NotNull(message = "재직 여부를 입력해주세요.")
    private Boolean isWorking;
    @NotBlank(message = "회사 이름을 입력해주세요.")
    @Size(max = 100, message = "회사 이름의 최대 길이는 100자입니다.")
    private String companyName;
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
