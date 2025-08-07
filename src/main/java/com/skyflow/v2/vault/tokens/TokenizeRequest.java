package com.skyflow.v2.vault.tokens;

import java.util.List;

public class TokenizeRequest {
    private final TokenizeRequestBuilder builder;

    private TokenizeRequest(TokenizeRequest.TokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static TokenizeRequestBuilder builder() {
        return new TokenizeRequestBuilder();
    }

    public List<ColumnValue> getColumnValues() {
        return this.builder.columnValues;
    }

    public static final class TokenizeRequestBuilder {
        private List<ColumnValue> columnValues;

        private TokenizeRequestBuilder() {
        }

        public TokenizeRequestBuilder values(List<ColumnValue> columnValues) {
            this.columnValues = columnValues;
            return this;
        }

        public TokenizeRequest build() {
            return new TokenizeRequest(this);
        }
    }
}
