/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class GetByIdRecordInput {
    private String[] ids;
    private String table;
    private String column_name;
    private String[] column_values;
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

    public String getColumn_name() {
        return column_name;
    }

    public void setColumn_name(String column_name) {
        this.column_name = column_name;
    }

    public String[] getColumn_values() {
        return column_values;
    }

    public void setColumn_values(String[] column_values) {
        this.column_values = column_values;
    }

    public RedactionType getRedaction() {
        return redaction;
    }

    public void setRedaction(RedactionType redaction) {
        this.redaction = redaction;
    }
}
