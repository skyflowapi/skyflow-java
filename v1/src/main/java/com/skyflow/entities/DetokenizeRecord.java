/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class DetokenizeRecord {
    private String token;
    private RedactionType redaction = RedactionType.PLAIN_TEXT;

    public String getToken() {
        return token;
    }

    void setToken(String token) {
        this.token = token;
    }

    public RedactionType getRedaction() {
        return redaction;
    }

    public void setRedaction(RedactionType redaction) {
        this.redaction = redaction;
    }
}
