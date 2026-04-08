package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class TokenizeSummary {
    @Expose(serialize = true)
    private int totalTokens;

    @Expose(serialize = true)
    private int totalTokenized;

    @Expose(serialize = true)
    private int totalPartial;

    @Expose(serialize = true)
    private int totalFailed;

    public TokenizeSummary() {}

    public TokenizeSummary(int totalTokens, int totalTokenized, int totalPartial, int totalFailed) {
        this.totalTokens = totalTokens;
        this.totalTokenized = totalTokenized;
        this.totalPartial = totalPartial;
        this.totalFailed = totalFailed;
    }

    public int getTotalTokens() { return totalTokens; }
    public int getTotalTokenized() { return totalTokenized; }
    public int getTotalPartial() { return totalPartial; }
    public int getTotalFailed() { return totalFailed; }

    @Override
    public String toString() {
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        return gson.toJson(this);
    }
}
