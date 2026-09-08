package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class QueryResponse {
    private final List<QueryResponseRecord> records;
    private final List<String> columns;

    public QueryResponse(List<QueryResponseRecord> records, List<String> columns) {
        this.records = records;
        this.columns = columns;
    }

    public List<QueryResponseRecord> getRecords() {
        return records;
    }

    /** The return columns for the query, when the vault reports them. */
    public List<String> getColumns() {
        return columns;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
