package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.Map;

public class DetokenizeResponseObject {
    @Expose(serialize = true)
    private int index;

    @Expose(serialize = true)
    private String token;
    @Expose(serialize = true)
    private Object value;
    @Expose(serialize = true)
    private String tokenGroupName;
    @Expose(serialize = true)
    private String error;

    @Expose(serialize = true)
    private Map<String, Object> metadata;

    public DetokenizeResponseObject(int index, String token, Object value, String tokenGroupName, String error, Map<String, Object> metadata) {
        this.token = token;
        this.value = value;
        this.tokenGroupName = tokenGroupName;
        this.error = error;
        this.metadata = metadata;
        this.index = index;
    }

    public String getToken() {
        return token;
    }

    public Object getValue() {
        return value;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getError() {
        return error;
    }

    public int getIndex() {
        return index;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

}
