package com.skyflow.errors;

public enum ErrorCodes {
    InvalidVaultURL(400, "Invalid Vault URL"),
    EmptyVaultID(400,"Empty Vault ID")
    ;
    private final int code;
    private final String description;
    ErrorCodes(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
