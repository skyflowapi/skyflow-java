package com.skyflow.vault.data;


import com.google.gson.Gson;

import java.util.Map;

public class DetokenizeRecordResponse extends BaseDetokenizeRecordResponse {
    private String tokenGroupName;
    private Map<String, Object> metadata;
    private Object value;

    public DetokenizeRecordResponse(String token, Object value, String error, String tokenGroupName, Map<String, Object> metadata) {
        super(token, error);
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.metadata = metadata;

    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }
    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public Object getValue(){
        return this.value;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}