package com.skyflow.v2.vault.detect;


public class Transformations {
    private final DateTransformation shiftDates;

    public Transformations(DateTransformation shiftDates) {
        this.shiftDates = shiftDates;
    }

    public DateTransformation getShiftDates() {
        return shiftDates;
    }
}
