package com.skyflow.vault.data;

import com.skyflow.enums.UpdateType;

import java.util.List;

public class UpdateRequest {
    private final UpdateRequestBuilder builder;

    protected UpdateRequest(UpdateRequestBuilder builder) {
        this.builder = builder;
    }

    public String getTableName() {
        return this.builder.tableName;
    }

    public List<UpdateRequestRecord> getRecords() {
        return this.builder.records;
    }

    /** If omitted, the vault treats it the same as {@link UpdateType#UPDATE}. */
    public UpdateType getUpdateType() {
        return this.builder.updateType;
    }

    public static UpdateRequestBuilder builder() {
        return new UpdateRequestBuilder();
    }

    public static class UpdateRequestBuilder {
        private String tableName;
        private List<UpdateRequestRecord> records;
        private UpdateType updateType;

        protected UpdateRequestBuilder() {
        }

        public UpdateRequestBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public UpdateRequestBuilder records(List<UpdateRequestRecord> records) {
            this.records = records;
            return this;
        }

        public UpdateRequestBuilder updateType(UpdateType updateType) {
            this.updateType = updateType;
            return this;
        }

        public UpdateRequest build() {
            return new UpdateRequest(this);
        }
    }
}
