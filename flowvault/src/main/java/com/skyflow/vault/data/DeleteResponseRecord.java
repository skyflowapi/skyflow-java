package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// The wire response (V1DeleteResponseObject) is simpler than insert/update/get's — just the
// deleted record's skyflowId plus a partial error, no data/tokens/hashedData — so this does not
// extend InsertResponseRecord.
public class DeleteResponseRecord {
    private final String skyflowId;
    private final Integer httpCode;
    private final String error;

    public DeleteResponseRecord(String skyflowId, Integer httpCode, String error) {
        this.skyflowId = skyflowId;
        this.httpCode = httpCode;
        this.error = error;
    }

    public String getSkyflowId() {
        return skyflowId;
    }

    public Integer getHttpCode() {
        return httpCode;
    }

    public String getError() {
        return error;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
