package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

public class InsertRecord {
    private final InsertRecordBuilder builder;

    private InsertRecord(InsertRecordBuilder builder) {
        this.builder = builder;
    }

    // Getters
    public String getTable() {
        return this.builder.table;
    }

    public Map<String, Object> getData() {
        return this.builder.data;
    }

    public List<String> getUpsert() {
        return this.builder.upsert;
    }

    public String getUpsertType() {
        return this.builder.upsertType;
    }

    // Builder Class
    public static final class InsertRecordBuilder {
        private String table;
        private Map<String, Object> data;
        private List<String> upsert;
        private String upsertType;

        public InsertRecordBuilder table(String table) {
            this.table = table;
            return this;
        }

        public InsertRecordBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public InsertRecordBuilder upsert(List<String> upsert) {
            this.upsert = upsert;
            return this;
        }

        public InsertRecordBuilder upsertType(String upsertType) {
            this.upsertType = upsertType;
            return this;
        }

        public InsertRecord build() {
            return new InsertRecord(this);
        }
    }

    // Static entry point for builder
    public static InsertRecordBuilder builder() {
        return new InsertRecordBuilder();
    }
}
