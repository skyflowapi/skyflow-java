package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;

// Response shape for the unary detokenize contract. Retained as published API even though the
// module currently exposes only the bulk operations.
public class DetokenizeResponse extends BaseDetokenizeResponse {
    private final List<DetokenizeResponseRecord> records;

    public DetokenizeResponse(List<DetokenizeResponseRecord> records) {
        this.records = records;
    }

    public List<DetokenizeResponseRecord> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
