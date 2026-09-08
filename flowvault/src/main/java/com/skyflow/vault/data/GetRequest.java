package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetRequest extends BaseGetRequest {
    private final GetRequestBuilder builder;

    protected GetRequest(GetRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public List<ColumnRedactions> getColumnRedactions() {
        return this.builder.columnRedactions;
    }

    /** Either this or {@code ids} is required in single-table mode; specifying both fails validation. */
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
     * (table/ids/fields/uniqueValues/columnRedactions) — specify one or the other, not both.
     */
    public List<GetRequestRecord> getRecords() {
        return this.builder.records;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public static final class GetRequestBuilder extends BaseGetRequestBuilder {
        private List<ColumnRedactions> columnRedactions;
        private List<Map<String, Object>> uniqueValues;
        private Integer limit;
        private Integer offset;
        private List<GetRequestRecord> records;

        private GetRequestBuilder() {
        }

        @Override
        public GetRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        @Override
        public GetRequestBuilder ids(ArrayList<String> ids) {
            super.ids(ids);
            return this;
        }

        @Override
        public GetRequestBuilder fields(ArrayList<String> fields) {
            super.fields(fields);
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
