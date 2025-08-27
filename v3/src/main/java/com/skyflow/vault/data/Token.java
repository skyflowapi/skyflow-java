package com.skyflow.vault.data;

import com.google.gson.annotations.Expose;

public class Token {
    @Expose(serialize = true)
    private String token;
    @Expose(serialize = true)
    private String tokenGroupName;

    public String getToken() {
        return token;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public Token(String token, String tokenGroupName) {
        this.token = token;
        this.tokenGroupName = tokenGroupName;
    }
}