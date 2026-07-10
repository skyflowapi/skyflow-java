package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;

public class DetokenizeData {
    private final String token;
    private final RedactionType redactionType;

    public DetokenizeData(String token) {
        this.token = token;
        this.redactionType = RedactionType.DEFAULT;
    }

    public DetokenizeData(String token, RedactionType redactionType) {
        this.token = token;
            this.redactionType = redactionType == null ? RedactionType.DEFAULT : redactionType;
    }

    public String getToken() {
        return this.token;
    }

    public RedactionType getRedactionType() {
        return this.redactionType;
    }
}
