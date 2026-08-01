package com.skyflow.vault.data;

import com.skyflow.enums.UpsertType;

import java.util.List;
import java.util.Map;

public class BulkInsertRecord {
    private final BulkInsertRecordBuilder builder;

    private BulkInsertRecord(BulkInsertRecordBuilder builder) {
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

    public UpsertType getUpsertType() {
        return this.builder.upsertType;
    }

    // Builder Class
    public static final class BulkInsertRecordBuilder {
        private String table;
        private Map<String, Object> data;
        private List<String> upsert;
        private UpsertType upsertType;

        public BulkInsertRecordBuilder table(String table) {
            this.table = table;
            return this;
        }

        public BulkInsertRecordBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public BulkInsertRecordBuilder upsert(List<String> upsert) {
            this.upsert = upsert;
            return this;
        }

        public BulkInsertRecordBuilder upsertType(UpsertType upsertType) {
            this.upsertType = upsertType;
            return this;
        }

        public BulkInsertRecord build() {
            return new BulkInsertRecord(this);
        }
    }

    // Static entry point for builder
    public static BulkInsertRecordBuilder builder() {
        return new BulkInsertRecordBuilder();
    }
}
