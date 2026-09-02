package com.zb.jogakjogak.security.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Schema(description = "회원 상세정보 응답 DTO")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class MemberResponseDto {

    @Schema(description = "회원 닉네임", example = "홍길동")
    private String nickname;
    @Schema(description = "회원 이메일", example = "user@example.com")
    private String email;
    @Schema(description = "이메일 알림 전체 활성화 여부", example = "true")
    private boolean isNotificationEnabled;

}
