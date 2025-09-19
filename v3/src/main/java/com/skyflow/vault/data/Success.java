package com.skyflow.vault.data;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import java.util.List;
import java.util.Map;

public class Success {
    @Expose(serialize = true)
    private int index;
    @Expose(serialize = true)
    private String skyflow_id;
    @Expose(serialize = true)
    private Map<String, List<Token>> tokens;
    @Expose(serialize = true)
    private Map<String, Object> data;
    @Expose(serialize = true)
    private String table;

    public int getIndex() {
        return index;
    }

    public Success(int index, String skyflow_id, Map<String, List<Token>> tokens, Map<String, Object> data, String table) {
        this.index = index;
        this.skyflow_id = skyflow_id;
        this.tokens = tokens;
        this.data = data;
    }

    public String getSkyflowId() {
        return this.skyflow_id;
    }

    public Map<String, List<Token>> getTokens() {
        return this.tokens;
    }

    public Map<String, Object> getData() {
        return this.data;
    }

    public String getTable(){
        return this.table;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}