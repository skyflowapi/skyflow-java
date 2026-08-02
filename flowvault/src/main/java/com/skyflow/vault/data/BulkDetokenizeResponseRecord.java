package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.Map;

// Bulk counterpart of DetokenizeResponseRecord. Adds the caller-facing position of the token
// in the submitted payload; all other fields are inherited.
public class BulkDetokenizeResponseRecord extends DetokenizeResponseRecord {
    private final int index;

    public BulkDetokenizeResponseRecord(int index, String token, Object value, String tokenGroupName,
                                        Map<String, Object> metadata, int httpCode, String error) {
        super(token, value, tokenGroupName, metadata, httpCode, error);
        this.index = index;
    }

    public int getIndex() {
        return index;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
