package com.skyflow.vault.data;

public class BulkTokenGroupRedactions {
    private final BulkTokenGroupRedactionsBuilder builder;

    private BulkTokenGroupRedactions(BulkTokenGroupRedactionsBuilder builder) {
        this.builder = builder;
    }
    public String getTokenGroupName() {
        return this.builder.tokenGroupName;
    }

    public String getRedaction() {
        return this.builder.redaction;
    }

    public static BulkTokenGroupRedactionsBuilder builder() {
        return new BulkTokenGroupRedactionsBuilder();
    }

    public static final class BulkTokenGroupRedactionsBuilder {
        private String tokenGroupName;
        private String redaction;

        public BulkTokenGroupRedactionsBuilder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        public BulkTokenGroupRedactionsBuilder redaction(String redaction) {
            this.redaction = redaction;
            return this;
        }

        public BulkTokenGroupRedactions build() {
            return new BulkTokenGroupRedactions(this);
        }
    }
}
