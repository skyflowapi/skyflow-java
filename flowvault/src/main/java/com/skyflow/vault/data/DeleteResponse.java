package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class DeleteResponse {
    private final List<DeleteResponseRecord> records;

    public DeleteResponse(List<DeleteResponseRecord> records) {
        this.records = records;
    }

    public List<DeleteResponseRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
