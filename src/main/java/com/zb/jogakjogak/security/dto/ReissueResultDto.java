package com.zb.jogakjogak.security.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "Access/Refresh Token 재발급 결과 DTO")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ReissueResultDto {

    @Schema(description = "재발급된 refresh token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJyZWZyZXNoIn0.xxxxx")
    private String newRefreshToken;
    @Schema(description = "재발급된 access token", example = "eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJhY2Nlc3MifQ.xxxxx")
    private String newAccessToken;
}
