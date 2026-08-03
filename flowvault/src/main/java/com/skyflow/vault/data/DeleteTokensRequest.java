package com.skyflow.vault.data;

import java.util.List;

public class DeleteTokensRequest {
    private final List<String> tokens;

    protected DeleteTokensRequest(List<String> tokens) {
        this.tokens = tokens;
    }

    public static DeleteTokensRequestBuilder builder() {
        return new DeleteTokensRequestBuilder();
    }

    public List<String> getTokens() {
        return this.tokens;
    }

    public static class DeleteTokensRequestBuilder {
        protected List<String> tokens;

        protected DeleteTokensRequestBuilder() {}

        public DeleteTokensRequestBuilder tokens(List<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        public DeleteTokensRequest build() {
            return new DeleteTokensRequest(this.tokens);
        }
    }
}
