package com.zb.jogakjogak.resume.domain.requestDto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SkillDto {
    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 255, message = "내용의 최대 길이는 255자입니다.")
    private String content;
}
