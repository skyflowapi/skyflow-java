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
        super(token, httpCode, error);
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
