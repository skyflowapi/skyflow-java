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

//    public void setTotalRecords(int totalRecords) {
//        this.totalRecords = totalRecords;
//    }

    public int getTotalInserted() {
        return totalInserted;
    }

//    public void setTotalInserted(int totalInserted) {
//        this.totalInserted = totalInserted;
//    }

    public int getTotalFailed() {
        return totalFailed;
    }

//    public void setTotalFailed(int totalFailed) {
//        this.totalFailed = totalFailed;
//    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}