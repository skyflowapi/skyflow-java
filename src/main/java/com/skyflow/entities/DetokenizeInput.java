package com.skyflow.entities;

public class DetokenizeInput {
    private DetokenizeRecord[] records;
    DetokenizeInput(){

    }
    public DetokenizeInput(DetokenizeRecord[] records) {
        this.records = records;
    }

    public DetokenizeRecord[] getRecords() {
        return records;
    }

    public void setRecords(DetokenizeRecord[] records) {
        this.records = records;
    }
}
