package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A single table's lookup within a multi-table {@link GetRequest#getRecords()} batch.
 * Mirrors the shape of a single-table {@link GetRequest}, minus the request-wide
 * {@code limit}/{@code offset}.
 */
public class GetRecordRequest extends BaseGetRequest {
    private final GetRecordRequestBuilder builder;

    private GetRecordRequest(GetRecordRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static GetRecordRequestBuilder builder() {
        return new GetRecordRequestBuilder();
    }

    public List<ColumnRedaction> getColumnRedactions() {
        return this.builder.columnRedactions;
    }

    public List<Map<String, Object>> getUniqueValues() {
        return this.builder.uniqueValues;
    }

    public static final class GetRecordRequestBuilder extends BaseGetRequestBuilder {
        private List<ColumnRedaction> columnRedactions;
        private List<Map<String, Object>> uniqueValues;

        private GetRecordRequestBuilder() {
        }

        @Override
        public GetRecordRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        @Override
        public GetRecordRequestBuilder ids(ArrayList<String> ids) {
            super.ids(ids);
            return this;
        }

        @Override
        public GetRecordRequestBuilder fields(ArrayList<String> fields) {
            super.fields(fields);
            return this;
        }

        public GetRecordRequestBuilder columnRedactions(List<ColumnRedaction> columnRedactions) {
            this.columnRedactions = columnRedactions;
            return this;
        }

        public GetRecordRequestBuilder uniqueValues(List<Map<String, Object>> uniqueValues) {
            this.uniqueValues = uniqueValues;
            return this;
        }

        public GetRecordRequest build() {
            return new GetRecordRequest(this);
        }
    }
}
