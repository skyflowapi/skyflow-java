package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GetRequest extends BaseGetRequest {
    private final GetRequestBuilder builder;

    private GetRequest(GetRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static GetRequestBuilder builder() {
        return new GetRequestBuilder();
    }

    public List<ColumnRedaction> getColumnRedactions() {
        return this.builder.columnRedactions;
    }

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
     * A multi-table lookup batch — when set, this request fetches from each listed
     * table in a single call instead of the single-table fields ({@code table}, {@code ids},
     * {@code fields}, {@code uniqueValues}, {@code columnRedactions}) above. The two modes
     * are mutually exclusive.
     */
    public List<GetRecordRequest> getRecords() {
        return this.builder.records;
    }

    public static final class GetRequestBuilder extends BaseGetRequestBuilder {
        private List<ColumnRedaction> columnRedactions;
        private List<Map<String, Object>> uniqueValues;
        private Integer limit;
        private Integer offset;
        private List<GetRecordRequest> records;

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

        public GetRequestBuilder columnRedactions(List<ColumnRedaction> columnRedactions) {
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

        public GetRequestBuilder records(List<GetRecordRequest> records) {
            this.records = records;
            return this;
        }

        public GetRequest build() {
            return new GetRequest(this);
        }
    }
}
