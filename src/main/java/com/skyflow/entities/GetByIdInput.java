package com.skyflow.entities;

public class GetByIdInput {
    private GetByIdRecordInput records[];

    public GetByIdRecordInput[] getRecords() {
        return records;
    }

    public void setRecords(GetByIdRecordInput[] records) {
        this.records = records;
    }
}
