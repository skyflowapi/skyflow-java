package com.skyflow.enums;

public enum DeidentifyFileStatus {
    IN_PROGRESS("IN_PROGRESS"),
    SUCCESS("SUCCESS");

    private final String value;

    DeidentifyFileStatus(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }
}