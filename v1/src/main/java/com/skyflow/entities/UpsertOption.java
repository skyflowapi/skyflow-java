/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.entities;

public class UpsertOption {
    private String table;
    private String column;

    public UpsertOption(String table, String column) {
        this.table = table;
        this.column = column;
    }

    public String getTable() {
        return table;
    }

    public String getColumn() {
        return column;
    }
}
