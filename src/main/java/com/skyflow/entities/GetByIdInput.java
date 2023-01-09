/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class GetByIdInput {
    private GetByIdRecordInput[] records;

    public GetByIdRecordInput[] getRecords() {
        return records;
    }

    public void setRecords(GetByIdRecordInput[] records) {
        this.records = records;
    }
}
