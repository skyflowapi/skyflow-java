package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Rows returned by a query. Values may be masked, but are never tokens or file URLs. Query has no
 * per-record status — any failure is thrown — so {@link #getErrors()} is always null.
 */
public class QueryResponse extends BaseQueryResponse {
    private final QueryResponseMetadata metadata;
    private final String requestId;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields, QueryResponseMetadata metadata, String requestId) {
        super(fields);
        this.metadata = metadata;
        this.requestId = requestId;
    }

    public QueryResponseMetadata getMetadata() {
        return metadata;
    }

    /** Call-level request id, populated on every response. */
    public String getRequestId() {
        return requestId;
    }
}
