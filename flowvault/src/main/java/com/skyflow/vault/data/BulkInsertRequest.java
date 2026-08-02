package com.skyflow.vault.data;

import java.util.List;

// Bulk counterpart of InsertRequest. Carries no extra state today; all fields
// (tableName, records, upsert) are inherited.
public class BulkInsertRequest extends InsertRequest {

    protected BulkInsertRequest(BulkInsertRequestBuilder builder) {
        super(builder);
    }

    public static BulkInsertRequestBuilder builder() {
        return new BulkInsertRequestBuilder();
    }

    public static final class BulkInsertRequestBuilder extends InsertRequestBuilder {

        private BulkInsertRequestBuilder() {
        }

        @Override
        public BulkInsertRequestBuilder tableName(String tableName) {
            super.tableName(tableName);
            return this;
        }

        @Override
        public BulkInsertRequestBuilder records(List<InsertRequestRecord> records) {
            super.records(records);
            return this;
        }

        @Override
        public BulkInsertRequestBuilder upsert(UpsertOptions upsert) {
            super.upsert(upsert);
            return this;
        }

        @Override
        public BulkInsertRequest build() {
            return new BulkInsertRequest(this);
        }
    }
}
