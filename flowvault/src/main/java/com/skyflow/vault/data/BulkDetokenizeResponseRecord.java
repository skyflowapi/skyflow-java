package com.skyflow.vault.data;

import com.google.gson.Gson;

// Bulk counterpart of DetokenizeResponseRecord. Adds the caller-facing position of the token
// in the submitted payload; all other fields are inherited.
public class BulkDetokenizeResponseRecord extends DetokenizeResponseRecord {
    private final int index;

    public BulkDetokenizeResponseRecord(int index, String token, Object value, String tokenGroupName,
                                        DetokenizeResponseRecordMetadata metadata, int httpCode, String error,
                                        String requestId) {
        // requestId is stored on DetokenizeResponseRecord (shared with the unary response), not
        // redeclared here — a same-named field on both this class and its parent breaks Gson's
        // reflective field walk (see ReflectiveTypeAdapterFactory.getBoundFields).
        super(token, value, tokenGroupName, metadata, httpCode, error, requestId);
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
