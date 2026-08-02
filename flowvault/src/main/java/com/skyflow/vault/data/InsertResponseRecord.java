package com.skyflow.vault.data;

import java.util.Map;

public class InsertResponseRecord {
    private final String tableName;
    private final String skyflowId;
    private final Map<String, Object> fields;
    private final Map<String, Object> hashedData;
    private final int httpCode;
    private final String error;

    public InsertResponseRecord(String tableName, String skyflowId, Map<String, Object> fields,
                                 Map<String, Object> hashedData, int httpCode, String error) {
        this.tableName = tableName;
        this.skyflowId = skyflowId;
        this.fields = fields;
        this.hashedData = hashedData;
        this.httpCode = httpCode;
        this.error = error;
    }

    public String getTableName() {
        return tableName;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public Map<String, Object> getFields() {
        return fields;
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
}
