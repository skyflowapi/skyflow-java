package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;

// Response shape for the unary insert contract. Retained as published API even though the
// module currently exposes only the bulk operations.
public class InsertResponse extends BaseInsertResponse {
    private final List<InsertResponseRecord> records;

    public InsertResponse(List<InsertResponseRecord> records) {
        this.records = records;
    }

    public List<InsertResponseRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
