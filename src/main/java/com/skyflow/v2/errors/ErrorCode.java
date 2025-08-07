package com.skyflow.v2.errors;

public enum ErrorCode {
    INVALID_INPUT(400),
    INVALID_INDEX(404),
    SERVER_ERROR(500),
    PARTIAL_SUCCESS(500);

    private final int code;

    ErrorCode(int code) {
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
