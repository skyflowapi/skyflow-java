package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * One token-group outcome for a single input value. {@code token} is populated on success and
 * {@code error} on failure; {@code httpCode} is present on both paths.
 */
public class TokenizeResponseToken {
    @Expose(serialize = true)
    private final String tokenGroupName;

    @Expose(serialize = true)
    private final String token;

    @Expose(serialize = true)
    private final Integer httpCode;

    @Expose(serialize = true)
    private final String error;

    public TokenizeResponseToken(String tokenGroupName, String token, Integer httpCode, String error) {
        this.tokenGroupName = tokenGroupName;
        this.token = token;
        this.httpCode = httpCode;
        this.error = error;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getToken() {
        return token;
    }

    public Integer getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
