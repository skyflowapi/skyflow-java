package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * One (value, token-group) outcome. {@code token} is populated on success and {@code error} on
 * failure; {@code httpCode} is present on both paths.
 *
 * <p>{@code requestId} identifies the API call this outcome came from and is set only when the
 * outcome is an error, since that is when it is useful for support.
 */
public class TokenizeResponseRecord {
    @Expose(serialize = true)
    private final Object value;

    @Expose(serialize = true)
    private final String tokenGroupName;

    @Expose(serialize = true)
    private final String token;

    @Expose(serialize = true)
    private final Integer httpCode;

    @Expose(serialize = true)
    private final String error;

    @Expose(serialize = true)
    private final String requestId;

    public TokenizeResponseRecord(Object value, String tokenGroupName, String token, Integer httpCode, String error) {
        this(value, tokenGroupName, token, httpCode, error, null);
    }

    public TokenizeResponseRecord(Object value, String tokenGroupName, String token, Integer httpCode,
                                  String error, String requestId) {
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.token = token;
        this.httpCode = httpCode;
        this.error = error;
        // a successful outcome carries no request id, whatever the caller passed
        this.requestId = error != null ? requestId : null;
    }

    public Object getValue() {
        return value;
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

    /** The API call this outcome came from; null unless this is an error. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}