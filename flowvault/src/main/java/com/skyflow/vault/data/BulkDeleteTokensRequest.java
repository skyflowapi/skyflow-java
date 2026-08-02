package com.skyflow.vault.data;

import java.util.List;

public class BulkDeleteTokensRequest extends DeleteTokensRequest {

    private BulkDeleteTokensRequest(List<String> tokens) {
        super(tokens);
    }

    public static BulkDeleteTokensRequestBuilder builder() {
        return new BulkDeleteTokensRequestBuilder();
    }

    public static final class BulkDeleteTokensRequestBuilder extends DeleteTokensRequestBuilder {

        private BulkDeleteTokensRequestBuilder() {}

        @Override
        public BulkDeleteTokensRequestBuilder tokens(List<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        @Override
        public BulkDeleteTokensRequest build() {
            return new BulkDeleteTokensRequest(this.tokens);
        }
    }
}
