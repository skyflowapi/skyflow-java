package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/** The token looked up for one value/token-group pair, or the error for that pair. */
public class GetTokensResponseRecord {
    private final Object value;
    private final String tokenGroupName;
    private final String token;
    private final int httpCode;
    private final String error;
    private final String requestId;

    public GetTokensResponseRecord(Object value, String tokenGroupName, String token, int httpCode, String error, String requestId) {
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.token = token;
        this.httpCode = httpCode;
        this.error = error;
        this.requestId = requestId;
    }

    public Object getValue() {
        return value;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    /** The token, or an empty string when none was found. */
    public String getToken() {
        return token;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }

    /** The API call this outcome came from; null unless this record carries an error. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
