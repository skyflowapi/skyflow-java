package com.skyflow.vault.data;

import com.google.gson.Gson;

// Bulk counterpart of DetokenizeResponseRecord. Adds the caller-facing position of the token
// in the submitted payload; all other fields are inherited.
public class BulkDetokenizeResponseRecord extends DetokenizeResponseRecord {
    private final int index;
    private final String requestId;

    public BulkDetokenizeResponseRecord(int index, String token, Object value, String tokenGroupName,
                                        DetokenizeMetadata metadata, int httpCode, String error,
                                        String requestId) {
        super(token, value, tokenGroupName, metadata, httpCode, error);
        this.index = index;
        this.requestId = requestId;
    }

    public int getIndex() {
        return index;
    }

    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
