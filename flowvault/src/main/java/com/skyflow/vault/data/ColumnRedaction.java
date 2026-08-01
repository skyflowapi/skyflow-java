package com.skyflow.vault.data;

public class ColumnRedaction {
    private final ColumnRedactionBuilder builder;

    private ColumnRedaction(ColumnRedactionBuilder builder) {
        this.builder = builder;
    }

    public String getColumnName() {
        return this.builder.columnName;
    }

    public String getRedaction() {
        return this.builder.redaction;
    }

    public static ColumnRedactionBuilder builder() {
        return new ColumnRedactionBuilder();
    }

    public static final class ColumnRedactionBuilder {
        private String columnName;
        private String redaction;

        public ColumnRedactionBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public ColumnRedactionBuilder redaction(String redaction) {
            this.redaction = redaction;
            return this;
        }

        public ColumnRedaction build() {
            return new ColumnRedaction(this);
        }
    }
}
