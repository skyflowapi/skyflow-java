package com.skyflow.vault.data;

import java.util.List;

public class QueryResponseMetadata {
    private final List<String> columns;

    public QueryResponseMetadata(List<String> columns) {
        this.columns = columns;
    }

    /** Column names returned by the query, in select order. */
    public List<String> getColumns() {
        return columns;
    }
}
