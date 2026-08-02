package com.skyflow.vault.data;

import java.util.List;

public class BulkDeleteTokensRequest {
    private final BulkDeleteTokensRequestBuilder builder;

    private BulkDeleteTokensRequest(BulkDeleteTokensRequestBuilder builder) {
        this.builder = builder;
    }

    public static BulkDeleteTokensRequestBuilder builder() {
        return new BulkDeleteTokensRequestBuilder();
    }

    public List<String> getTokens() {
        return this.builder.tokens;
    }

    public static final class BulkDeleteTokensRequestBuilder {
        private List<String> tokens;

        private BulkDeleteTokensRequestBuilder() {}

        public BulkDeleteTokensRequestBuilder tokens(List<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        public BulkDeleteTokensRequest build() {
            return new BulkDeleteTokensRequest(this);
        }
    }
}
