package com.skyflow.vault.data;
import java.util.List;
import java.util.Map;

public class Success {
    private String index;
    private String skyflow_id;
    private Map<String, List<Token>> tokens;
    private Map<String, String> data;

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
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

    public Map<String, String> getData() {
        return data;
    }

    public void setData(Map<String, String> data) {
        this.data = data;
    }
}