package com.skyflow.enums;

public enum CustomHeaderKey {
    SkyflowAccountID("x-skyflow-account-id"),
    SkyflowAccountName("x-skyflow-account-name"),
    RequestIDHeader("x-request-id");

    private final String value;

    CustomHeaderKey(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}