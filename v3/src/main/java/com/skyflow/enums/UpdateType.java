package com.skyflow.enums;

public enum UpdateType {
    UPDATE("UPDATE"),

    REPLACE("REPLACE");

    private final String value;

    UpdateType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
