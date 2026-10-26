package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class BaseDeleteFilesResponse {
    private final ArrayList<HashMap<String, Object>> records;

    public BaseDeleteFilesResponse(ArrayList<HashMap<String, Object>> records) {
        this.records = records;
    }

    /**
     * Returns one record map per resolved record. A record addressed by uniqueValues can resolve to
     * several records, so there may be more entries than input records. Each map carries the
     * skyflowId, table, the deleted columns (null on a failed record) and the per-record
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
