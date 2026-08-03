package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * A {@link DeleteTokensRecord} carrying the token's position in the original bulk request.
 */
public class BulkDeleteTokensResponseRecord extends DeleteTokensRecord {
    @Expose(serialize = true)
    private final int index;

    public BulkDeleteTokensResponseRecord(int index, String token, Integer httpCode, String error) {
        this(index, token, httpCode, error, null);
    }

    public BulkDeleteTokensResponseRecord(int index, String token, Integer httpCode,
                                          String error, String requestId) {
        super(token, httpCode, error, requestId);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
