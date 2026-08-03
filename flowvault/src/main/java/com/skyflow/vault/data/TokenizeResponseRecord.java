package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * The tokenization outcome for one input value: every requested token group is reported in
 * {@code tokens}, whether it succeeded or failed.
 */
public class TokenizeResponseRecord {
    @Expose(serialize = true)
    private final Object value;

    @Expose(serialize = true)
    private final List<TokenizeResponseToken> tokens;

    public TokenizeResponseRecord(Object value, List<TokenizeResponseToken> tokens) {
        this.value = value;
        this.tokens = tokens;
    }

    public Object getValue() {
        return value;
    }

    public List<TokenizeResponseToken> getTokens() {
        return tokens;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
