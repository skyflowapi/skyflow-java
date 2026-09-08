package com.skyflow.vault.data;

public class ColumnRedactions {
    private final ColumnRedactionsBuilder builder;

    private ColumnRedactions(ColumnRedactionsBuilder builder) {
        this.builder = builder;
    }

    public String getColumnName() {
        return this.builder.columnName;
    }

    public String getRedaction() {
        return this.builder.redaction;
    }

    public static ColumnRedactionsBuilder builder() {
        return new ColumnRedactionsBuilder();
    }

    public static final class ColumnRedactionsBuilder {
        private String columnName;
        private String redaction;

        public ColumnRedactionsBuilder columnName(String columnName) {
            this.columnName = columnName;
            return this;
        }

        public ColumnRedactionsBuilder redaction(String redaction) {
            this.redaction = redaction;
            return this;
        }

        public ColumnRedactions build() {
            return new ColumnRedactions(this);
        }
    }
}
