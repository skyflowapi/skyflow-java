package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.List;

public class TokenizeResponse {
    private final List<TokenizeResponseRecord> response;

    public TokenizeResponse(List<TokenizeResponseRecord> response) {
        this.response = response;
    }

    public List<TokenizeResponseRecord> getResponse() {
        return response;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
