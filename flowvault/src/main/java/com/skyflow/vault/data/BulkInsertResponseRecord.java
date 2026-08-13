package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.Map;

// Bulk counterpart of InsertResponseRecord. Adds the caller-facing position of the record
// in the submitted payload; all other fields are inherited.
public class BulkInsertResponseRecord extends InsertResponseRecord {
    private final int index;
    private final String requestId;

    /**
     * @deprecated Use {@link #BulkInsertResponseRecord(int, String, String, Map, Map, Map, int, String, String)}
     * instead, which also lets you populate {@code data}. This overload always leaves {@code data} null.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, Object> tokens, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        this(index, tableName, skyflowId, tokens, null, hashedData, httpCode, error, requestId);
    }

    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, Object> tokens, Map<String, Object> data, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        super(tableName, skyflowId, tokens, data, hashedData, httpCode, error);
        this.index = index;
        this.requestId = requestId;
    }

    public int getIndex() {
        return index;
    }

    public String getRequestId(){
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
