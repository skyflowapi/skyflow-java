package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

// Response record for the unary insert method. Deliberately does NOT carry the deprecated
// getFields()/6-arg-constructor back-compat surface that BulkInsertResponseRecord still has to —
// this is a brand-new type with no pre-1.0.2 callers to support, so it stays clean.
public class InsertResponseRecord {
    private final String tableName;
    private final String skyflowId;
    private final Map<String, List<Token>> tokens;
    private final Map<String, Object> data;
    private final Map<String, Object> hashedData;
    private final int httpCode;
    private final String error;
    private final String requestId;

    public InsertResponseRecord(String tableName, String skyflowId, Map<String, List<Token>> tokens,
                                 Map<String, Object> data, Map<String, Object> hashedData, int httpCode, String error,
                                 String requestId) {
        this.tableName = tableName;
        this.skyflowId = skyflowId;
        this.tokens = tokens;
        this.data = data;
        this.hashedData = hashedData;
        this.httpCode = httpCode;
        this.error = error;
        this.requestId = requestId;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    /**
     * Per-column token data. The API models a column's tokens generically (see
     * {@link Token#parseTokens(Map)}), but the SDK parses that into {@link Token} objects here
     * so callers get {@link Token#getToken()}/{@link Token#getTokenGroupName()} directly, with no
     * casting required.
     */
    public Map<String, List<Token>> getTokens() {
        return tokens;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public Map<String, Object> getHashedData() {
        return hashedData;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }

    /** The API call this outcome came from; null unless this is an error. */
    public String getRequestId() {
        return requestId;
    }
}
