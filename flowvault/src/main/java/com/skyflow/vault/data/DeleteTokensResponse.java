package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class DeleteTokensResponse {
    private final List<DeleteTokensRecord> records;

    public DeleteTokensResponse(List<DeleteTokensRecord> records) {
        this.records = records;
    }

    public List<DeleteTokensRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
