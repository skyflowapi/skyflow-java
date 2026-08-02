package com.skyflow.vault.data;

import java.util.Map;

// Bulk counterpart of InsertRequestRecord. Carries no extra state today; it exists so
// bulk-only fields can be added without touching the unary record.
public class BulkInsertRequestRecord extends InsertRequestRecord {

    protected BulkInsertRequestRecord(BulkInsertRequestRecordBuilder builder) {
        super(builder);
    }

    public static BulkInsertRequestRecordBuilder builder() {
        return new BulkInsertRequestRecordBuilder();
    }

    public static final class BulkInsertRequestRecordBuilder extends InsertRequestRecordBuilder {

        private BulkInsertRequestRecordBuilder() {
        }

        @Override
        public BulkInsertRequestRecordBuilder tableName(String tableName) {
            super.tableName(tableName);
            return this;
        }

        @Override
        public BulkInsertRequestRecordBuilder data(Map<String, Object> data) {
            super.data(data);
            return this;
        }

        @Override
        public BulkInsertRequestRecordBuilder tokens(Map<String, Object> tokens) {
            super.tokens(tokens);
            return this;
        }

        @Override
        public BulkInsertRequestRecordBuilder upsert(UpsertOptions upsert) {
            super.upsert(upsert);
            return this;
        }

        @Override
        public BulkInsertRequestRecord build() {
            return new BulkInsertRequestRecord(this);
        }
    }
}
