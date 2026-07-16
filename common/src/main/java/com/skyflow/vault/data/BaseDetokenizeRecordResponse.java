package com.skyflow.vault.data;

public class BaseDetokenizeRecordResponse {
    private final String token;
    private final String error;

    public BaseDetokenizeRecordResponse(String token, String error){
        this.token = token;
        this.error = error;
    }
    public String getToken() {
        return token;
    }

    public String getError() {
        return error;
    }

}
