package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

public class DetokenizeSummary {
    @Expose(serialize = true)
    private int totalTokens;
    @Expose(serialize = true)
    private int totalDetokenized;
    @Expose(serialize = true)
    private int totalFailed;

    public DetokenizeSummary() {
    }

    public DetokenizeSummary(int totalTokens, int totalDetokenized, int totalFailed) {
        this.totalTokens = totalTokens;
        this.totalDetokenized = totalDetokenized;
        this.totalFailed = totalFailed;
    }

    public int getTotalTokens() {
        return totalTokens;
    }

    public int getTotalDetokenized() {
        return totalDetokenized;
    }

    public int getTotalFailed() {
        return totalFailed;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
