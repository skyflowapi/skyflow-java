package com.skyflow.common.errors;

public enum HttpStatus {
    BAD_REQUEST("Bad Request");

    private final String httpStatus;

    HttpStatus(String httpStatus) {
        this.httpStatus = httpStatus;
    }

    public String getHttpStatus() {
        return httpStatus;
    }
}
