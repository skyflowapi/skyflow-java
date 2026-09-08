package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class GetResponse {
    private final List<GetResponseRecord> records;

    public GetResponse(List<GetResponseRecord> records) {
        this.records = records;
    }

    public List<GetResponseRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
