package com.skyflow.vault.data;

import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

public class Summary {
    @Expose(serialize = true)
    private int totalRecords;
    @Expose(serialize = true)
    private int totalInserted;
    @Expose(serialize = true)
    private int totalFailed;

    public Summary() {
    }

    public Summary(int totalRecords, int totalInserted, int totalFailed) {
        this.totalRecords = totalRecords;
        this.totalInserted = totalInserted;
        this.totalFailed = totalFailed;
    }

    public int getTotalRecords() {
        return totalRecords;
    }


    public int getTotalInserted() {
        return totalInserted;
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