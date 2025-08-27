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

//    public void setToken(String token) {
//        this.token = token;
//    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public void setTokenGroupName(String tokenGroupName) {
        this.tokenGroupName = tokenGroupName;
    }


}