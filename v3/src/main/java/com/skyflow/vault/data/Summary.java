package com.skyflow.vault.data;

import com.google.gson.Gson;

public class Summary {
    private int total_records;
    private int total_inserted;
    private int total_failed;

    public int getTotalRecords() {
        return total_records;
    }

    public void setTotalRecords(int total_records) {
        this.total_records = total_records;
    }

    public int getTotalInserted() {
        return total_inserted;
    }

    public void setTotalInserted(int total_inserted) {
        this.total_inserted = total_inserted;
    }

    public int getTotalFailed() {
        return total_failed;
    }

    public void setTotalFailed(int total_failed) {
        this.total_failed = total_failed;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}