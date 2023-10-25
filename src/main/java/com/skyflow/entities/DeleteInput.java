package com.skyflow.entities;

public class DeleteInput {
    private DeleteRecordInput[] records;

    public DeleteRecordInput[] getRecords() {
        return records;
    }

    public void setRecords(DeleteRecordInput[] records) {
        this.records = records;
    }
}
