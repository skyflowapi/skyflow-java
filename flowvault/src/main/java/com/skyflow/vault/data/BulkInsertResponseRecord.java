package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.Map;

// Bulk counterpart of InsertResponseRecord. Adds the caller-facing position of the record
// in the submitted payload; all other fields are inherited.
public class BulkInsertResponseRecord extends InsertResponseRecord {
    private final int index;

    public BulkInsertResponseRecord(int index, String tableName, String skyflowId,
                                    Map<String, Object> fields, Map<String, Object> hashedData,
                                    int httpCode, String error) {
        super(tableName, skyflowId, fields, hashedData, httpCode, error);
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
