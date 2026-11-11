package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

/**
 * One record whose file columns to delete. Set exactly one of {@code skyflowId} or
 * {@code uniqueValues}; a record that sets both or neither fails validation.
 */
public class DeleteFilesRequestRecord {
    private final String tableName;
    private final List<String> columns;
    private final String skyflowId;
    private final List<Map<String, Object>> uniqueValues;

    protected DeleteFilesRequestRecord(DeleteFilesRequestRecordBuilder builder) {
        this.tableName = builder.tableName;
        this.columns = builder.columns;
        this.skyflowId = builder.skyflowId;
        this.uniqueValues = builder.uniqueValues;
    }

    public static DeleteFilesRequestRecordBuilder builder() {
        return new DeleteFilesRequestRecordBuilder();
    }

    public String getTableName() {
        return this.tableName;
    }

    /** File column names to delete. */
    public List<String> getColumns() {
        return this.columns;
    }

    public String getSkyflowId() {
        return this.skyflowId;
    }

    /**
     * Unique-column maps, e.g. {@code [{"email": "a@b.com"}]}. Each resolves independently and may
     * match more than one record, giving one response entry per resolved skyflowId.
     */
    public List<Map<String, Object>> getUniqueValues() {
        return this.uniqueValues;
    }

    public static class DeleteFilesRequestRecordBuilder {
        protected String tableName;
        protected List<String> columns;
        protected String skyflowId;
        protected List<Map<String, Object>> uniqueValues;

        protected DeleteFilesRequestRecordBuilder() {}

        public DeleteFilesRequestRecordBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public DeleteFilesRequestRecordBuilder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public DeleteFilesRequestRecordBuilder skyflowId(String skyflowId) {
            this.skyflowId = skyflowId;
            return this;
        }

        public DeleteFilesRequestRecordBuilder uniqueValues(List<Map<String, Object>> uniqueValues) {
            this.uniqueValues = uniqueValues;
            return this;
        }

        public DeleteFilesRequestRecord build() {
            return new DeleteFilesRequestRecord(this);
        }
    }
}
