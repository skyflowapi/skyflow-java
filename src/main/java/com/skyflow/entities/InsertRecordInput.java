/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

import org.json.simple.JSONObject;

public class InsertRecordInput {
    private String table;
    private JSONObject fields;

    public String getTable() {
        return table;
    }

    public void setTable(String table) {
        this.table = table;
    }

    public JSONObject getFields() {
        return fields;
    }

    public void setFields(JSONObject fields) {
        this.fields = fields;
    }
}
