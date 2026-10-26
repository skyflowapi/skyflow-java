package com.skyflow.vault.data;

public class GetTokensRequestRecord {
    private final Object value;
    private final String tokenGroupName;

    protected GetTokensRequestRecord(GetTokensRequestRecordBuilder builder) {
        this.value = builder.value;
        this.tokenGroupName = builder.tokenGroupName;
    }

    public static GetTokensRequestRecordBuilder builder() {
        return new GetTokensRequestRecordBuilder();
    }

    public Object getValue() {
        return this.value;
    }

    /** Name of a deterministic token group; non-deterministic groups are not supported. */
    public String getTokenGroupName() {
        return this.tokenGroupName;
    }

    public static class GetTokensRequestRecordBuilder {
        protected Object value;
        protected String tokenGroupName;

        protected GetTokensRequestRecordBuilder() {}

        public GetTokensRequestRecordBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public GetTokensRequestRecordBuilder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        public GetTokensRequestRecord build() {
            return new GetTokensRequestRecord(this);
        }
    }
}
