package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

// One table's worth of lookup fields inside GetRequest#getRecords() (the multi-table mode).
// Mirrors GetRequest's own single-table fields, minus limit/offset, which only apply once for
// the whole call.
public class GetRequestRecord {
    private final GetRequestRecordBuilder builder;

    protected GetRequestRecord(GetRequestRecordBuilder builder) {
        this.builder = builder;
    }

    public String getTable() {
        return this.builder.table;
    }

    public List<String> getIds() {
        return this.builder.ids;
    }

    public List<String> getFields() {
        return this.builder.fields;
    }

    public List<ColumnRedactions> getColumnRedactions() {
        return this.builder.columnRedactions;
    }

    public List<Map<String, Object>> getUniqueValues() {
        return this.builder.uniqueValues;
    }

    public static GetRequestRecordBuilder builder() {
        return new GetRequestRecordBuilder();
    }

    public static final class GetRequestRecordBuilder {
        private String table;
        private List<String> ids;
        private List<String> fields;
        private List<ColumnRedactions> columnRedactions;
        private List<Map<String, Object>> uniqueValues;

        protected GetRequestRecordBuilder() {
        }

        public GetRequestRecordBuilder table(String table) {
            this.table = table;
            return this;
        }

        public GetRequestRecordBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        public GetRequestRecordBuilder fields(List<String> fields) {
            this.fields = fields;
            return this;
        }

        public GetRequestRecordBuilder columnRedactions(List<ColumnRedactions> columnRedactions) {
            this.columnRedactions = columnRedactions;
            return this;
        }

        public GetRequestRecordBuilder uniqueValues(List<Map<String, Object>> uniqueValues) {
            this.uniqueValues = uniqueValues;
            return this;
        }

        public GetRequestRecord build() {
            return new GetRequestRecord(this);
        }
    }
}
