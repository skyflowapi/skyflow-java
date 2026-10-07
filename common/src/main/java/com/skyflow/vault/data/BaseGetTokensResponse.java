package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseGetTokensResponse {
    private final ArrayList<HashMap<String, Object>> records;

    public BaseGetTokensResponse(ArrayList<HashMap<String, Object>> records) {
        this.records = records;
    }

    /**
     * Returns one record map per input entry, in request order. Each map carries the looked-up
     * value, its token group, the token (or null), and the per-record status/error.
     */
    public ArrayList<HashMap<String, Object>> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
