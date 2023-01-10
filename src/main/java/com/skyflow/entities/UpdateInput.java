package com.skyflow.entities;

public class UpdateInput {
    private UpdateRecordInput[] records;

    public UpdateRecordInput[] getRecords() {
        return records;
    }

    public void setRecords(UpdateRecordInput[] records) {
        this.records = records;
    }
}
