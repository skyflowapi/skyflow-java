package com.skyflow.vault.data;

import java.util.Map;

public class InsertRequestRecord {
    private final InsertRequestRecordBuilder builder;

    protected InsertRequestRecord(InsertRequestRecordBuilder builder) {
        this.builder = builder;
    }

    // Getters
    public String getTableName() {
        return this.builder.tableName;
    }

    public Map<String, Object> getData() {
        return this.builder.data;
    }

    public UpsertOptions getUpsert() {
        return this.builder.upsert;
    }

    // Builder Class
    public static class InsertRequestRecordBuilder {
        private String tableName;
        private Map<String, Object> data;
        private UpsertOptions upsert;

        public InsertRequestRecordBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public InsertRequestRecordBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public InsertRequestRecordBuilder upsert(UpsertOptions upsert) {
            this.upsert = upsert;
            return this;
        }

        public InsertRequestRecord build() {
            return new InsertRequestRecord(this);
        }
    }

    // Static entry point for builder
    public static InsertRequestRecordBuilder builder() {
        return new InsertRequestRecordBuilder();
    }
}
