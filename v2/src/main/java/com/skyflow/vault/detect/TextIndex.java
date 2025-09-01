package com.skyflow.vault.detect;


import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class TextIndex {
    private final Integer start;
    private final Integer end;

    public TextIndex(Integer start, Integer end) {
        this.start = start;
        this.end = end;
    }

    public int getStart() {
        return start;
    }

    public int getEnd() {
        return end;
    }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}
