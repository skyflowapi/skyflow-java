package com.skyflow.vault.data;

import com.skyflow.enums.TokenMode;

import java.util.HashMap;

public class UpdateRequest {
    private final UpdateRequestBuilder builder;

    private UpdateRequest(UpdateRequestBuilder builder) {
        this.builder = builder;
    }

    public static UpdateRequestBuilder builder() {
        return new UpdateRequestBuilder();
    }

    public String getTable() {
        return this.builder.table;
    }

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

    public HashMap<String, Object> getData() {
        return this.builder.data;
    }

    public HashMap<String, Object> getTokens() {
        return this.builder.tokens;
    }

    public TokenMode getTokenMode() {
        return this.builder.tokenMode;
    }

    public static final class UpdateRequestBuilder {
        private String table;
        private Boolean returnTokens;
        private HashMap<String, Object> data;
        private HashMap<String, Object> tokens;
        private TokenMode tokenMode;

        private UpdateRequestBuilder() {
            this.returnTokens = true;
            this.tokenMode = TokenMode.DISABLE;
        }

        public UpdateRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public UpdateRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens == null || returnTokens;
            return this;
        }

        public UpdateRequestBuilder data(HashMap<String, Object> data) {
            this.data = data;
            return this;
        }

        public UpdateRequestBuilder tokens(HashMap<String, Object> tokens) {
            this.tokens = tokens;
            return this;
        }

        public UpdateRequestBuilder tokenMode(TokenMode tokenStrict) {
            this.tokenMode = tokenStrict == null ? TokenMode.DISABLE : tokenStrict;
            return this;
        }

        public UpdateRequest build() {
            return new UpdateRequest(this);
        }
    }
}
