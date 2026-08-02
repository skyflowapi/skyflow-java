package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * A single token's delete outcome. {@code token} and {@code httpCode} are present on both the
 * success and the error path; {@code error} is null when the token was deleted successfully.
 *
 * <p>{@code requestId} identifies the API call this outcome came from and is set only when the
 * outcome is an error, since that is when it is useful for support. Bulk requests are split into
 * batches, so every error from the same batch carries the same id and errors from different
 * batches carry different ones.
 */
public class DeleteTokensRecord {
    @Expose(serialize = true)
    private final String token;

    @Expose(serialize = true)
    private final Integer httpCode;

    @Expose(serialize = true)
    private final String error;

    @Expose(serialize = true)
    private final String requestId;

    public DeleteTokensRecord(String token, Integer httpCode, String error) {
        this(token, httpCode, error, null);
    }

    public DeleteTokensRecord(String token, Integer httpCode, String error, String requestId) {
        this.token = token;
        this.httpCode = httpCode;
        this.error = error;
        // a successful delete carries no request id, whatever the caller passed
        this.requestId = error != null ? requestId : null;
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
