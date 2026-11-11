package com.skyflow.vault.data;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rows returned by a query. Values may be masked, but are never tokens or file URLs. Query has no
 * per-record status: any failure is thrown as a SkyflowException.
 */
public class QueryResponse extends BaseQueryResponse {
    private final QueryResponseMetadata metadata;
    private final String requestId;

    public QueryResponse(ArrayList<HashMap<String, Object>> fields, QueryResponseMetadata metadata, String requestId) {
        super(fields);
        this.metadata = metadata;
        this.requestId = requestId;
    }

    /**
     * The rows the query returned, one map of column name to value per row, in the column order the
     * vault returned. Never null; read-only. The vault returns at most 25 rows per call, so page with
     * {@code LIMIT} and {@code OFFSET} in the query.
     */
    public List<Map<String, Object>> getRecords() {
        List<HashMap<String, Object>> rows = super.getFields();
        return rows == null ? Collections.<Map<String, Object>>emptyList() : Collections.<Map<String, Object>>unmodifiableList(rows);
    }

    /**
     * The rows the query returned.
     *
     * @deprecated The name is misleading: these are rows, not fields. Use {@link #getRecords()}.
     */
    @Deprecated
    @Override
    public ArrayList<HashMap<String, Object>> getFields() {
        return super.getFields();
    }

    /**
     * Always null: query has no per-record errors, and any failure is thrown.
     *
     * @deprecated Carries no information. Catch SkyflowException for query failures instead.
     */
    @Deprecated
    @Override
    public ArrayList<HashMap<String, Object>> getErrors() {
        return super.getErrors();
    }

    public QueryResponseMetadata getMetadata() {
        return metadata;
    }

    /** Call-level request id, populated on every response. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Map<String, Object> view = new LinkedHashMap<>();
        view.put("records", getRecords());
        view.put("metadata", metadata);
        view.put("requestId", requestId);
        return new GsonBuilder().serializeNulls().create().toJson(view);
    }
}
