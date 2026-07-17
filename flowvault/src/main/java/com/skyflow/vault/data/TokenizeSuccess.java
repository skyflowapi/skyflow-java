package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Represents one successfully tokenized input record.
 * All tokens generated for this record (one per tokenGroupName) are grouped
 * under the {@code tokens} map: tokenGroupName → token value.
 */
public class TokenizeSuccess {
    @Expose(serialize = true)
    private int index;

    @Expose(serialize = true)
    private Object value;

    @Expose(serialize = true)
    private Map<String, String> tokens;

    public TokenizeSuccess(int index, Object value) {
        this.index = index;
        this.value = value;
        this.tokens = new LinkedHashMap<>();
    }

    /** Adds a token for a specific token group name. */
    public void addToken(String tokenGroupName, String token) {
        this.tokens.put(tokenGroupName, token);
    }

    public int getIndex() {
        return index;
    }

    public Object getValue() {
        return value;
    }

    public Map<String, String> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
