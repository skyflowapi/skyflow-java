package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

public class ErrorRecord {
    @Expose(serialize = true)
    private int index;
    @Expose(serialize = true)
    private String error;
    @Expose(serialize = true)
    private int code;
//    public ErrorRecord() {
//    }

    public ErrorRecord(int index, String error, int code) {
        this.index = index;
        this.error = error;
        this.code = code;
    }
    public String getError() {
        return error;
    }

    public int getCode() {
        return code;
    }

    public int getIndex() {
        return index;
    }


    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}