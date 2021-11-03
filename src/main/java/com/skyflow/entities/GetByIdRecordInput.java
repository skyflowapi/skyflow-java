package com.skyflow.entities;

public class GetByIdRecordInput {
    private String[] ids;
    private String table;
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

    public RedactionType getRedaction() {
        return redaction;
    }

    public void setRedaction(RedactionType redaction) {
        this.redaction = redaction;
    }
}
