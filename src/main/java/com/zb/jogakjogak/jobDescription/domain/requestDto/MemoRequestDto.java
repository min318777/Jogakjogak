package com.zb.jogakjogak.jobDescription.domain.requestDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "분석 메모 작성 요청 DTO")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoRequestDto {
    @Schema(description = "메모", example = "1일차 - 조각 3개 완료, 2일차 - 조각 2개 완료", requiredMode = Schema.RequiredMode.REQUIRED)
    @Size(max = 1000, message = "메모의 최대 길이는 1000자입니다.")
    private String memo;
}
