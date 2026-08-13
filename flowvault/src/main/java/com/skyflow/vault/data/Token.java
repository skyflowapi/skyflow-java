package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

/**
 * One token-group outcome for a single column value, as returned inside
 * {@link InsertResponseRecord#getTokens()}. A column tokenized against more than one
 * token group comes back as a list of these, one per group; see
 * {@link InsertResponseRecord#getTokenDetails()} for the typed accessor that parses them.
 */
public class Token {
    @Expose(serialize = true)
    private final String token;
    @Expose(serialize = true)
    private final String tokenGroupName;

    public Token(String token, String tokenGroupName) {
        this.token = token;
        this.tokenGroupName = tokenGroupName;
    }

    public String getToken() {
        return token;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}