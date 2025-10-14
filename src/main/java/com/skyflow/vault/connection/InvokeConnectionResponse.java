package com.skyflow.vault.connection;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.HashMap;

public class InvokeConnectionResponse {
    private final Object data;
    private final HashMap<String, String> metadata;
    private final ArrayList<HashMap<String, Object>> errors;

    public InvokeConnectionResponse(Object data, HashMap<String, String> metadata, ArrayList<HashMap<String, Object>> errors) {
        this.data = data;
        this.metadata = metadata;
        this.errors = errors;
    }

    public Object getData() {
        return data;
    }

    public HashMap<String, String> getMetadata() {
        return metadata;
    }

    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
