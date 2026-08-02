package com.skyflow.vault.data;

import java.util.List;

public class InsertRequest extends BaseInsertRequest {
    private final InsertRequestBuilder builder;

    protected InsertRequest(InsertRequestBuilder builder) {
        this.builder = builder;
    }

    public static InsertRequestBuilder builder() {
        return new InsertRequestBuilder();
    }

    public String getTableName() {
        return this.builder.tableName;
    }

    public List<InsertRequestRecord> getRecords() {
        return this.builder.records;
    }

    public UpsertOptions getUpsert() {
        return this.builder.upsert;
    }

    public static class InsertRequestBuilder {
        private String tableName;
        private List<InsertRequestRecord> records;
        private UpsertOptions upsert;

        protected InsertRequestBuilder() {
        }

        public InsertRequestBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public InsertRequestBuilder records(List<InsertRequestRecord> records) {
            this.records = records;
            return this;
        }

        public InsertRequestBuilder upsert(UpsertOptions upsert) {
            this.upsert = upsert;
            return this;
        }

        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
