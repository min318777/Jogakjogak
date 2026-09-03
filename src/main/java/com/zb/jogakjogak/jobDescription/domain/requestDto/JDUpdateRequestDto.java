package com.zb.jogakjogak.jobDescription.domain.requestDto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.time.LocalDateTime;

@Schema(description = "JD 수정 요청 DTO (보낸 필드만 부분 수정됩니다)")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JDUpdateRequestDto {
    @Schema(description = "생성할 분석 내용의 제목", example = "J사 백엔드 취업 해보자!", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 30, message = "제목의 최대 길이는 30자입니다.")
    private String title;
    @Schema(description = "채용 공고의 URL", example = "https://jogakjogak.com", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @URL(message = "유효한 URL 형식이 아닙니다.")
    @Size(max = 1000, message = "URL의 최대 길이는 1000자입니다.")
    private String jdUrl;
    @Schema(description = "채용 공고의 회사명", example = "Jogakjogak", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 30, message = "회사 이름의 최대 길이는 30자입니다.")
    private String companyName;
    @Schema(description = "지원하는 직무명", example = "백엔드 개발자", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    @Size(max = 30, message = "직무 이름의 최대 길이는 30자입니다.")
    private String job;
    @Schema(description = "채용 공고의 마감일", example = "2025-06-22T10:30:00Z", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
    private LocalDateTime endedAt;
}
