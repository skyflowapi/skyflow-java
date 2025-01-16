package com.skyflow.vault.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.HashMap;

public class InvokeConnectionResponse {
    private final Object data;
    private final HashMap<String, String> metadata;

    public InvokeConnectionResponse(Object data, HashMap<String, String> metadata) {
        this.data = data;
        this.metadata = metadata;
    }

    public Object getData() {
        return data;
    }

    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
