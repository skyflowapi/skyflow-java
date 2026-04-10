package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

public class DeleteTokensSuccess {
    @Expose(serialize = true)
    private int index;

    @Expose(serialize = true)
    private String token;

    public DeleteTokensSuccess(int index, String token) {
        this.index = index;
        this.token = token;
    }

    public int getIndex() {
        return index;
    }

    public String getToken() {
        return token;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
