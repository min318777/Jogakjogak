package com.zb.jogakjogak.security.dto;


import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

@Schema(description = "회원 상세정보 수정 요청 DTO")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class UpdateMemberRequestDto {

    @Schema(description = "수정할 닉네임 (4자 이상 12자 이하, 이미 사용 중이면 409 반환)", example = "홍길동")
    @Size(min = 4, max = 12, message = "닉네임은 최소 4자 이상, 최대 12자 이하이어야 합니다.")
    private String nickname;
    @Schema(description = "이메일 알림 전체 활성화 여부", example = "true")
    private Boolean isNotificationEnabled;

}
