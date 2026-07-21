package com.skyflow.enums;

public enum UpsertType {
    UPDATE("UPDATE"),

    REPLACE("REPLACE");

    private final String value;

    UpsertType(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value;
    }
}
