package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

public class GetRequest extends BaseGetRequest {
    private final GetRequestBuilder builder;

    protected GetRequest(GetRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public String getTableName() {
        return this.builder.tableName;
    }

    public List<String> getSkyflowIds() {
        return this.builder.skyflowIds;
    }

    public List<String> getColumns() {
        return this.builder.columns;
    }

    public List<ColumnRedactions> getColumnRedactions() {
        return this.builder.columnRedactions;
    }

    /** Either this or {@code skyflowIds} is required in single-table mode; specifying both fails validation. */
    public List<Map<String, Object>> getUniqueValues() {
        return this.builder.uniqueValues;
    }

    public Integer getLimit() {
        return this.builder.limit;
    }

    public Integer getOffset() {
        return this.builder.offset;
    }

    /**
     * Multi-table lookup: mutually exclusive with the single-table fields above
     * (tableName/skyflowIds/columns/uniqueValues/columnRedactions) — specify one or the other, not both.
     */
    public List<GetRequestRecord> getRecords() {
        return this.builder.records;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public static final class GetRequestBuilder extends BaseGetRequestBuilder {
        private String tableName;
        private List<String> skyflowIds;
        private List<String> columns;
        private List<ColumnRedactions> columnRedactions;
        private List<Map<String, Object>> uniqueValues;
        private Integer limit;
        private Integer offset;
        private List<GetRequestRecord> records;

        private GetRequestBuilder() {
        }

        public GetRequestBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public GetRequestBuilder skyflowIds(List<String> skyflowIds) {
            this.skyflowIds = skyflowIds;
            return this;
        }

        public GetRequestBuilder columns(List<String> columns) {
            this.columns = columns;
            return this;
        }

        public GetRequestBuilder columnRedactions(List<ColumnRedactions> columnRedactions) {
            this.columnRedactions = columnRedactions;
            return this;
        }

        public GetRequestBuilder uniqueValues(List<Map<String, Object>> uniqueValues) {
            this.uniqueValues = uniqueValues;
            return this;
        }

        public GetRequestBuilder limit(Integer limit) {
            this.limit = limit;
            return this;
        }

        public GetRequestBuilder offset(Integer offset) {
            this.offset = offset;
            return this;
        }

        public GetRequestBuilder records(List<GetRequestRecord> records) {
            this.records = records;
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
