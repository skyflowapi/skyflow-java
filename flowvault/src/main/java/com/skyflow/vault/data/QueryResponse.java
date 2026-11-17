package com.skyflow.vault.data;

import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Rows returned by a query. Values may be masked, but are never tokens or file URLs. Query has no
 * per-record status: any failure is thrown as a SkyflowException.
 * <p>
 * This is the flowvault query response and stands on its own: it does not extend the shared
 * {@code BaseQueryResponse}, whose {@code getFields()} / {@code getErrors()} shape belongs to the
 * skyvault SDK and was never part of the flowvault contract.
 */
public class QueryResponse {
    private final List<Map<String, Object>> records;
    private final QueryResponseMetadata metadata;
    private final String requestId;

    public QueryResponse(List<? extends Map<String, Object>> records, QueryResponseMetadata metadata, String requestId) {
        this.records = records == null
                ? Collections.<Map<String, Object>>emptyList()
                : Collections.unmodifiableList(new ArrayList<Map<String, Object>>(records));
        this.metadata = metadata;
        this.requestId = requestId;
    }

    /**
     * The rows the query returned, one map of column name to value per row, in the column order the
     * vault returned. Never null; read-only. The vault returns at most 25 rows per call, so page with
     * {@code LIMIT} and {@code OFFSET} in the query.
     */
    public List<Map<String, Object>> getRecords() {
        return records;
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
        view.put("records", records);
        view.put("metadata", metadata);
        view.put("requestId", requestId);
        return new GsonBuilder().serializeNulls().create().toJson(view);
    }
}
