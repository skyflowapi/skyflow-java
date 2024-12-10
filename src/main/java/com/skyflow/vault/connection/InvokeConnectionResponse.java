package com.skyflow.vault.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class InvokeConnectionResponse {
    private final JsonObject data;
    private final JsonObject metadata;

    public InvokeConnectionResponse(JsonObject data, JsonObject metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public JsonObject getData() {
        return data;
    }

    public JsonObject getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
