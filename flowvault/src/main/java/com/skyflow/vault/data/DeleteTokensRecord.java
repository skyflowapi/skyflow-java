package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * A single token's delete outcome. {@code token} and {@code httpCode} are present on both the
 * success and the error path; {@code error} is null when the token was deleted successfully.
 */
public class DeleteTokensRecord {
    @Expose(serialize = true)
    private final String token;

    @Expose(serialize = true)
    private final Integer httpCode;

    @Expose(serialize = true)
    private final String error;

    public DeleteTokensRecord(String token, Integer httpCode, String error) {
        this.token = token;
        this.httpCode = httpCode;
        this.error = error;
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
