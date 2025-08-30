package com.skyflow.vault.data;

import com.google.gson.Gson;

public class DetokenizeSummary {
    private int totalTokens;
    private int totalDetokenized;
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
