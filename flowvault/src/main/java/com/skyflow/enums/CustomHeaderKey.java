package com.skyflow.enums;

public enum CustomHeaderKey {
    SKYFLOW_ACCOUNT_ID("x-skyflow-account-id"),
    SKYFLOW_ACCOUNT_NAME("x-skyflow-account-name"),
    REQUEST_ID_HEADER("x-request-id");

    private final String value;

    CustomHeaderKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
