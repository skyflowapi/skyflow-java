package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class UpdateResponse {
    private final List<UpdateResponseRecord> records;

    public UpdateResponse(List<UpdateResponseRecord> records) {
        this.records = records;
    }

    public List<UpdateResponseRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
