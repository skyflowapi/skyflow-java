package com.skyflow.vault.data;

import java.util.List;

public class UploadFilesRequestRecord {
    private final String tableName;
    private final String skyflowId;
    private final List<UploadFilesRequestColumn> columns;

    protected UploadFilesRequestRecord(UploadFilesRequestRecordBuilder builder) {
        this.tableName = builder.tableName;
        this.skyflowId = builder.skyflowId;
        this.columns = builder.columns;
    }

    public static UploadFilesRequestRecordBuilder builder() {
        return new UploadFilesRequestRecordBuilder();
    }

    public String getTableName() {
        return this.tableName;
    }

    /** Record to upload into. Omit to create a new record (CREATE permission); set to update one (UPDATE permission). */
    public String getSkyflowId() {
        return this.skyflowId;
    }

    public List<UploadFilesRequestColumn> getColumns() {
        return this.columns;
    }

    public static class UploadFilesRequestRecordBuilder {
        protected String tableName;
        protected String skyflowId;
        protected List<UploadFilesRequestColumn> columns;

        protected UploadFilesRequestRecordBuilder() {}

        public UploadFilesRequestRecordBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public UploadFilesRequestRecordBuilder skyflowId(String skyflowId) {
            this.skyflowId = skyflowId;
            return this;
        }

        public UploadFilesRequestRecordBuilder columns(List<UploadFilesRequestColumn> columns) {
            this.columns = columns;
            return this;
        }

        public UploadFilesRequestRecord build() {
            return new UploadFilesRequestRecord(this);
        }
    }
}
