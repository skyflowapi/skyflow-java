package com.skyflow.v2.vault.tokens;

import com.skyflow.v2.enums.RedactionType;

public class DetokenizeData {
    private final String token;
    private final RedactionType redactionType;

    public DetokenizeData(String token) {
        this.token = token;
        this.redactionType = RedactionType.PLAIN_TEXT;
    }

    public DetokenizeData(String token, RedactionType redactionType) {
        this.token = token;
        this.redactionType = redactionType == null ? RedactionType.PLAIN_TEXT : redactionType;
    }

    public String getToken() {
        return this.token;
    }

    public RedactionType getRedactionType() {
        return this.redactionType;
    }
}
