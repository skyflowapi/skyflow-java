package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

// Wraps the query's return columns, mirroring the wire shape (metadata.columns) directly instead
// of a flat columns field bolted onto QueryResponse.
public class QueryResponseMetadata {
    private final List<String> columns;

    public QueryResponseMetadata(List<String> columns) {
        this.columns = columns;
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
