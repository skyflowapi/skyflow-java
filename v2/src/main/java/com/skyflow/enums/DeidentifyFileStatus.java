package com.skyflow.enums;

public enum DeidentifyFileStatus {
    IN_PROGRESS("IN_PROGRESS"),
    FAILED("FAILED"),
    SUCCESS("SUCCESS"),
    UNKNOWN("UNKNOWN");

    private final String value;

    DeidentifyFileStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}