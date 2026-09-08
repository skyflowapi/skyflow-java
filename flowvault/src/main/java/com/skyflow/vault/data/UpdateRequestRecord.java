package com.skyflow.vault.data;

import java.util.Map;

public class UpdateRequestRecord {
    private final UpdateRequestRecordBuilder builder;

    protected UpdateRequestRecord(UpdateRequestRecordBuilder builder) {
        this.builder = builder;
    }

    public String getSkyflowId() {
        return this.builder.skyflowId;
    }

    public Map<String, Object> getData() {
        return this.builder.data;
    }

    public Map<String, Object> getTokens() {
        return this.builder.tokens;
    }

    /** Overrides UpdateRequest#getTableName() for this record only. */
    public String getTableName() {
        return this.builder.tableName;
    }

    public static UpdateRequestRecordBuilder builder() {
        return new UpdateRequestRecordBuilder();
    }

    public static class UpdateRequestRecordBuilder {
        private String skyflowId;
        private Map<String, Object> data;
        private Map<String, Object> tokens;
        private String tableName;

        protected UpdateRequestRecordBuilder() {
        }

        public UpdateRequestRecordBuilder skyflowId(String skyflowId) {
            this.skyflowId = skyflowId;
            return this;
        }

        public UpdateRequestRecordBuilder data(Map<String, Object> data) {
            this.data = data;
            return this;
        }

        public UpdateRequestRecordBuilder tokens(Map<String, Object> tokens) {
            this.tokens = tokens;
            return this;
        }

        public UpdateRequestRecordBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public UpdateRequestRecord build() {
            return new UpdateRequestRecord(this);
        }
    }
}
