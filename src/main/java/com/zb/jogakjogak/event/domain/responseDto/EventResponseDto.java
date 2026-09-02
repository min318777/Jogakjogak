package com.zb.jogakjogak.event.domain.responseDto;

import com.zb.jogakjogak.event.entity.Event;
import com.zb.jogakjogak.event.type.EventType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "이벤트 조회 응답 DTO")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class EventResponseDto {
    @Schema(description = "이벤트 id", example = "1")
    private Long id;
    @Schema(description = "이벤트 코드", example = "new-member-2025")
    private String code;
    @Schema(description = "이벤트 타입", example = "NEW_MEMBER")
    private EventType type;
    @Schema(description = "최초 조회 여부 (최초 조회 시 true, 이후 조회부터는 false)", example = "true")
    private Boolean isFirst;

    public static EventResponseDto from(Event event) {
        return EventResponseDto.builder()
                .id(event.getId())
                .code(event.getCode())
                .type(event.getType())
                .isFirst(event.getIsFirst())
                .build();
    }
}
