package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

// Bulk counterpart of InsertResponseRecord. Adds the caller-facing position of the record
// in the submitted payload; all other fields are inherited.
public class BulkInsertResponseRecord extends InsertResponseRecord {
    private final int index;

    /**
     * @deprecated Use {@link #BulkInsertResponseRecord(int, String, String, Map, Map, Map, int, String, String)}
     * instead, which also lets you populate {@code data}. This overload always leaves {@code data} null.
     */
    @Deprecated(since = "1.0.2", forRemoval = true)
    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, List<Token>> tokens, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        this(index, tableName, skyflowId, tokens, null, hashedData, httpCode, error, requestId);
    }

    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, List<Token>> tokens, Map<String, Object> data, Map<String, Object> hashedData,
                                    int httpCode, String error, String requestId) {
        // requestId is stored on InsertResponseRecord (shared with the unary response), not
        // redeclared here — a same-named field on both this class and its parent breaks Gson's
        // reflective field walk (see ReflectiveTypeAdapterFactory.getBoundFields).
        super(tableName, skyflowId, tokens, data, hashedData, httpCode, error, requestId);
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
