package com.skyflow.vault.data;

import java.util.List;

public class BulkTokenizeRecord {
    private final BulkTokenizeRecordBuilder builder;

    private BulkTokenizeRecord(BulkTokenizeRecordBuilder builder) {
        this.builder = builder;
    }

    public static BulkTokenizeRecordBuilder builder() {
        return new BulkTokenizeRecordBuilder();
    }

    public Object getValue() {
        return this.builder.value;
    }

    public List<String> getTokenGroupNames() {
        return this.builder.tokenGroupNames;
    }

    public static final class BulkTokenizeRecordBuilder {
        private Object value;
        private List<String> tokenGroupNames;

        private BulkTokenizeRecordBuilder() {}

        public BulkTokenizeRecordBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public BulkTokenizeRecordBuilder tokenGroupNames(List<String> tokenGroupNames) {
            this.tokenGroupNames = tokenGroupNames;
            return this;
        }

        public BulkTokenizeRecord build() {
            return new BulkTokenizeRecord(this);
        }
    }
}
