package com.skyflow.vault.data;

import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;

public class FileUploadResponse {
    private final String skyflowId;
    private final ArrayList<HashMap<String, Object>> errors;

    public FileUploadResponse(String skyflowId, ArrayList<HashMap<String, Object>> errors) {
        this.skyflowId = skyflowId;
        this.errors = errors;
    }

    public String getSkyflowId() {
        return skyflowId;
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
