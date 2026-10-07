package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseUploadFilesResponse {
    private final ArrayList<HashMap<String, Object>> records;

    public BaseUploadFilesResponse(ArrayList<HashMap<String, Object>> records) {
        this.records = records;
    }

    /**
     * Returns one record map per input record, in request order. Each map carries the record's
     * skyflowId and table, the upload status of every requested column, and the per-record
     * status/error.
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
