package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;

/**
 * @param <R> the SDK-specific record type
 */
public class BaseUploadFilesResponse<R> {
    private final List<R> records;

    public BaseUploadFilesResponse(List<R> records) {
        this.records = records;
    }

    /**
     * Returns one record per input record, in request order. Each record carries the record's
     * skyflowId and table, the upload status of every requested column, and the per-record
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
