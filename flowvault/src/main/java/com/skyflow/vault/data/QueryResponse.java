package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class QueryResponse {
    private final List<QueryResponseRecord> records;
    private final QueryResponseMetadata metadata;

    public QueryResponse(List<QueryResponseRecord> records, QueryResponseMetadata metadata) {
        this.records = records;
        this.metadata = metadata;
    }

    public List<QueryResponseRecord> getRecords() {
        return records;
    }

    /** Wraps the query's return columns, mirroring the wire shape (metadata.columns) directly. */
    public QueryResponseMetadata getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
