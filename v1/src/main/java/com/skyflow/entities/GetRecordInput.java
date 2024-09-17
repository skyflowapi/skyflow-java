/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class GetRecordInput {
    private String[] ids;
    private String table;
    private String columnName;
    private String[] columnValues;
    private RedactionType redaction;

    public String[] getIds() {
        return ids;
    }

    public void setIds(String[] ids) {
        this.ids = ids;
    }

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String[] getColumnValues() {
        return columnValues;
    }

    public void setColumnValues(String[] columnValues) {
        this.columnValues = columnValues;
    }

    public RedactionType getRedaction() {
        return redaction;
    }

    public void setRedaction(RedactionType redaction) {
        this.redaction = redaction;
    }
}
