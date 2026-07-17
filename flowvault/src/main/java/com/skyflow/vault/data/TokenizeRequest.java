package com.skyflow.vault.data;

import java.util.ArrayList;

public class TokenizeRequest {
    private final TokenizeRequestBuilder builder;

    private TokenizeRequest(TokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static TokenizeRequestBuilder builder() {
        return new TokenizeRequestBuilder();
    }

    public ArrayList<TokenizeRecord> getData() {
        return this.builder.data;
    }

    public static final class TokenizeRequestBuilder {
        private ArrayList<TokenizeRecord> data;

        private TokenizeRequestBuilder() {}

        public TokenizeRequestBuilder data(ArrayList<TokenizeRecord> data) {
            this.data = data;
            return this;
        }

        public TokenizeRequest build() {
            return new TokenizeRequest(this);
        }
    }
}
