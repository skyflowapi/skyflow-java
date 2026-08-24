package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * A {@link TokenizeResponseRecord} carrying the index of the input value it belongs to. The index
 * is assigned by the SDK from the matching {@link BulkTokenizeRequestRecord}'s position.
 */
public class BulkTokenizeResponseRecord extends TokenizeResponseRecord {
    @Expose(serialize = true)
    private final int index;

    public BulkTokenizeResponseRecord(int index, Object value, String tokenGroupName, String token,
                                      Integer httpCode, String error, String requestId) {
        super(value, tokenGroupName, token, httpCode, error, requestId);
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