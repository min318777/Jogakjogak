package com.zb.jogakjogak.global.exception;

import java.util.Map;
import java.util.UUID;

public record ErrorResponse(String errorCode, String message, String traceId, Map<String, Object> details) {

    public ErrorResponse(String errorCode, String message) {
        this(errorCode, message, UUID.randomUUID().toString(), null);
    }

    public ErrorResponse(String errorCode, String message, Map<String, Object> details) {
        this(errorCode, message, UUID.randomUUID().toString(), details);
    }
}
