package com.zb.jogakjogak.global;

public record CommonResponse<T>(T data, String message) {
}