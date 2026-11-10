package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

/** The upload outcome of one requested record, with one result per requested column. */
public class UploadFilesResponseRecord {
    private final String skyflowId;
    private final String tableName;
    private final List<UploadFilesColumnResult> columns;
    private final int httpCode;
    private final String error;
    private final String requestId;

    public UploadFilesResponseRecord(String skyflowId, String tableName, List<UploadFilesColumnResult> columns, int httpCode, String error, String requestId) {
        this.skyflowId = skyflowId;
        this.tableName = tableName;
        this.columns = columns;
        this.httpCode = httpCode;
        this.error = error;
        this.requestId = requestId;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public String getTableName() {
        return tableName;
    }

    public List<UploadFilesColumnResult> getColumns() {
        return columns;
    }

    public int getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }

    /** The API call this outcome came from; null unless this record carries an error. */
    public String getRequestId() {
        return requestId;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
