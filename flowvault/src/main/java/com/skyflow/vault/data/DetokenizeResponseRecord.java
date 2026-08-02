package com.skyflow.vault.data;

import java.util.Map;

public class DetokenizeResponseRecord extends BaseDetokenizeRecordResponse {
    // Passed straight through from V1FlowDetokenizeResponseObject.getValue() (Optional<Object>).
    private final Object value;
    private final String tokenGroupName;
    private final Map<String, Object> metadata;
    private final int httpCode;

    public DetokenizeResponseRecord(String token, Object value, String tokenGroupName,
                                     Map<String, Object> metadata, int httpCode, String error) {
        super(token, error);
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.metadata = metadata;
        this.httpCode = httpCode;
    }

    public Object getValue() {
        return value;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public int getHttpCode() {
        return httpCode;
    }
}
