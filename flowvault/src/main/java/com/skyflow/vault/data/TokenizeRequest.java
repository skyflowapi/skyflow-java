package com.skyflow.vault.data;

import java.util.List;

public class TokenizeRequest {
    private final List<? extends TokenizeRequestRecord> records;

    protected TokenizeRequest(List<? extends TokenizeRequestRecord> records) {
        this.records = records;
    }

    public static TokenizeRequestBuilder builder() {
        return new TokenizeRequestBuilder();
    }

    public List<? extends TokenizeRequestRecord> getRecords() {
        return this.records;
    }

    public static class TokenizeRequestBuilder {
        protected List<? extends TokenizeRequestRecord> records;

        protected TokenizeRequestBuilder() {}

        public TokenizeRequestBuilder records(List<? extends TokenizeRequestRecord> records) {
            this.records = records;
            return this;
        }

        public TokenizeRequest build() {
            return new TokenizeRequest(this.records);
        }
    }
}
