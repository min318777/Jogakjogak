package com.zb.jogakjogak.security.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원 온보딩 상태 수정 응답 DTO")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateIsOnboardedResponseDto {

    @Schema(description = "온보딩 완료 여부", example = "true")
    private boolean isOnboarded;
}
