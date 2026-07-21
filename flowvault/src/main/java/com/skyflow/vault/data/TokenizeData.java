package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.LinkedHashMap;
import java.util.Map;

public class TokenizeData {
    private Object value;
    private Map<String, String> tokens;
    private int index;

    public TokenizeData(Object value, int index){
        this.value = value;
        this.tokens = new LinkedHashMap<>();
        this.index = index;
    }
    public void addToken(String tokenGroupName, String token) {
        this.tokens.put(tokenGroupName, token);
    }

    public Object getValue() {
        return value;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().create();
        return gson.toJson(this);
    }
}
