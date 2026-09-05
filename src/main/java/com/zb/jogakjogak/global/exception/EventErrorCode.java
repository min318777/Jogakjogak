package com.zb.jogakjogak.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum EventErrorCode {
    NOT_FOUND_EVENT_CODE(HttpStatus.BAD_REQUEST, "이벤트 코드를 찾을 수 없습니다." ),
    FAILED_TO_GENERATE_EVENT_CODE(HttpStatus.INTERNAL_SERVER_ERROR, "이벤트 코드 생성에 실패했습니다." ),
    ;
    private final HttpStatus httpStatus;
    private final String message;
}
