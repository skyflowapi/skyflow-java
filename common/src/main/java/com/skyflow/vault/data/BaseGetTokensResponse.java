package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.List;

/**
 * @param <R> the SDK-specific record type
 */
public class BaseGetTokensResponse<R> {
    private final List<R> records;

    public BaseGetTokensResponse(List<R> records) {
        this.records = records;
    }

    /**
     * Returns one record per input entry, in request order. Each record carries the looked-up
     * value, its token group, the token (or "" when there is none), and the per-record status/error.
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
