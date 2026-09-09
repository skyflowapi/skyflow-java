package com.skyflow.vault.data;

public class DetokenizeResponseRecord extends BaseDetokenizeRecordResponse {
    // Passed straight through from V1FlowDetokenizeResponseObject.getValue() (Optional<Object>).
    private final Object value;
    private final String tokenGroupName;
    private final DetokenizeMetadata metadata;
    private final int httpCode;
    private final String requestId;

    public DetokenizeResponseRecord(String token, Object value, String tokenGroupName,
                                     DetokenizeMetadata metadata, int httpCode, String error) {
        this(token, value, tokenGroupName, metadata, httpCode, error, null);
    }

    public DetokenizeResponseRecord(String token, Object value, String tokenGroupName,
                                     DetokenizeMetadata metadata, int httpCode, String error, String requestId) {
        super(token, error);
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.metadata = metadata;
        this.httpCode = httpCode;
        this.requestId = requestId;
    }

    public Object getValue() {
        return value;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    /**
     * The record's skyflowId/tableName, typed. The API models this generically (see
     * {@link DetokenizeMetadata#parseMetadata(java.util.Map)}), but the SDK parses it here so
     * callers get {@link DetokenizeMetadata#getSkyflowId()}/{@link DetokenizeMetadata#getTableName()}
     * directly, with no casting required.
     */
    public DetokenizeMetadata getMetadata() {
        return metadata;
    }

    public int getHttpCode() {
        return httpCode;
    }

    /** The API call this outcome came from; null unless this is an error. */
    public String getRequestId() {
        return requestId;
    }
}
