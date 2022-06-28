/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class InsertInput {

    private InsertRecordInput[] records;

    public InsertRecordInput[] getRecords() {
        return records;
    }

    public void setRecords(InsertRecordInput[] records) {
        this.records = records;
    }
}
