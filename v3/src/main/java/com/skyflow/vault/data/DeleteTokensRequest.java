package com.skyflow.vault.data;

import java.util.List;

public class DeleteTokensRequest {
    private final DeleteTokensRequestBuilder builder;

    private DeleteTokensRequest(DeleteTokensRequestBuilder builder) {
        this.builder = builder;
    }

    public static DeleteTokensRequestBuilder builder() {
        return new DeleteTokensRequestBuilder();
    }

    public List<String> getTokens() {
        return this.builder.tokens;
    }

    public static final class DeleteTokensRequestBuilder {
        private List<String> tokens;

        private DeleteTokensRequestBuilder() {}

        public DeleteTokensRequestBuilder tokens(List<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        public DeleteTokensRequest build() {
            return new DeleteTokensRequest(this);
        }
    }
}
