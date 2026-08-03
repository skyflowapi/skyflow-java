package com.skyflow.vault.data;

import java.util.List;

// Bulk counterpart of DetokenizeRequest. Carries no extra state today; all fields
// (tokens, tokenGroupRedactions) are inherited.
public class BulkDetokenizeRequest extends DetokenizeRequest {

    protected BulkDetokenizeRequest(BulkDetokenizeRequestBuilder builder) {
        super(builder);
    }

    public static BulkDetokenizeRequestBuilder builder() {
        return new BulkDetokenizeRequestBuilder();
    }

    public static final class BulkDetokenizeRequestBuilder extends DetokenizeRequestBuilder {

        private BulkDetokenizeRequestBuilder() {
        }

        @Override
        public BulkDetokenizeRequestBuilder tokens(List<String> tokens) {
            super.tokens(tokens);
            return this;
        }

        @Override
        public BulkDetokenizeRequestBuilder tokenGroupRedactions(List<TokenGroupRedactions> tokenGroupRedactions) {
            super.tokenGroupRedactions(tokenGroupRedactions);
            return this;
        }

        @Override
        public BulkDetokenizeRequest build() {
            return new BulkDetokenizeRequest(this);
        }
    }
}
