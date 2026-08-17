package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.Map;

/**
 * Typed shape of a detokenize record's {@code metadata}. The wire response nests the record's
 * skyflow id and table name here, under the literal keys {@code skyflowID} and {@code table}
 * (see {@code flowdb_dp_apis.proto}) — {@link #parseMetadata(Map)} normalizes both into the
 * camelCase, {@code tableName}-shaped accessors below.
 */
public class DetokenizeMetadata {
    @Expose(serialize = true)
    private final String skyflowId;
    @Expose(serialize = true)
    private final String tableName;

    public DetokenizeMetadata(String skyflowId, String tableName) {
        this.skyflowId = skyflowId;
        this.tableName = tableName;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public String getTableName() {
        return tableName;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }

    /**
     * Parses the raw wire-shaped metadata map (keys {@code skyflowID}/{@code skyflowId} and
     * {@code table}/{@code tableName} — the API has been observed to send either casing) into a
     * {@link DetokenizeMetadata}. Returns {@code null} for {@code null} input, matching the
     * record-level metadata field being absent entirely on error records.
     */
    public static DetokenizeMetadata parseMetadata(Map<String, Object> rawMetadata) {
        if (rawMetadata == null) {
            return null;
        }
        Object skyflowId = rawMetadata.containsKey("skyflowId") ? rawMetadata.get("skyflowId") : rawMetadata.get("skyflowID");
        Object tableName = rawMetadata.containsKey("tableName") ? rawMetadata.get("tableName") : rawMetadata.get("table");
        return new DetokenizeMetadata(
                skyflowId != null ? skyflowId.toString() : null,
                tableName != null ? tableName.toString() : null);
    }
}
