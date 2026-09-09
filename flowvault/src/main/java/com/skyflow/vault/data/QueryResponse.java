package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class QueryResponse {
    private final List<QueryResponseRecord> records;
    private final List<String> columns;
    private final String requestId;

    public QueryResponse(List<QueryResponseRecord> records, List<String> columns, String requestId) {
        this.records = records;
        this.columns = columns;
        this.requestId = requestId;
    }

    public List<QueryResponseRecord> getRecords() {
        return records;
    }

    /** The return columns for the query, when the vault reports them. */
    public List<String> getColumns() {
        return columns;
    }

    // Query rows carry no per-record error, so unlike insert/update/get/delete/detokenize
    // there is no error-gated per-record id: a QueryResponse only ever exists for a
    // successful call, so its requestId is always populated from that call's headers.
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
