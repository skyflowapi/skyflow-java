package com.skyflow.v2.vault.tokens;

public class ColumnValue {

    private final ColumnValueBuilder builder;

    private ColumnValue(ColumnValueBuilder builder) {
        this.builder = builder;
    }

    public static ColumnValueBuilder builder() {
        return new ColumnValueBuilder();
    }

    public String getValue() {
        return this.builder.value;
    }

    public String getColumnGroup() {
        return this.builder.columnGroup;
    }

    public static final class ColumnValueBuilder {
        private String value;
        private String columnGroup;

        private ColumnValueBuilder() {
        }

        public ColumnValueBuilder value(String value) {
            this.value = value;
            return this;
        }

        public ColumnValueBuilder columnGroup(String columnGroup) {
            this.columnGroup = columnGroup;
            return this;
        }

        public ColumnValue build() {
            return new ColumnValue(this);
        }
    }

}
