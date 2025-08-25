package com.skyflow.vault.data;
import com.google.gson.Gson;

import java.util.List;
import java.util.Map;

public class Success {
    private int index;
    private String skyflow_id;
    private Map<String, List<Token>> tokens;
    private Map<String, Object> data;

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getSkyflowId() {
        return skyflow_id;
    }

    public void setSkyflowId(String skyflow_id) {
        this.skyflow_id = skyflow_id;
    }

    public Map<String, List<Token>> getTokens() {
        return tokens;
    }

    public void setTokens(Map<String, List<Token>> tokens) {
        this.tokens = tokens;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}