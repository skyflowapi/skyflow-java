package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;

/**
 * @param <R> the SDK-specific record type
 */
public class BaseDeleteFilesResponse<R> {
    private final List<R> records;

    public BaseDeleteFilesResponse(List<R> records) {
        this.records = records;
    }

    /**
     * Returns one record per resolved record. A record addressed by uniqueValues can resolve to
     * several records, so there may be more entries than input records. Each record carries the
     * skyflowId, table, the deleted columns (null on a failed record) and the per-record
     * status/error.
     */
    public List<R> getRecords() {
        return records;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
