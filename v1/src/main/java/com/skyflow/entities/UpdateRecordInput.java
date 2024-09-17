package com.skyflow.entities;

import org.json.simple.JSONObject;

public class UpdateRecordInput {
        private String id;
        private String table;
        private JSONObject fields;

        public String getTable() {
            return table;
        }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
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
