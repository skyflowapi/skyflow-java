package com.skyflow.vault.data;

import com.skyflow.enums.TokenMode;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertRequest extends BaseInsertRequest {
    private final InsertRequestBuilder builder;

    private InsertRequest(InsertRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static InsertRequestBuilder builder() {
        return new InsertRequestBuilder();
    }

    public Boolean getHomogeneous() {
        return this.builder.homogeneous;
    }

    public Boolean getContinueOnError() {
        return this.builder.continueOnError;
    }

    public TokenMode getTokenMode() {
        return this.builder.tokenMode;
    }

    public static final class InsertRequestBuilder extends BaseInsertRequestBuilder {
        private Boolean homogeneous;
        private Boolean continueOnError;
        private TokenMode tokenMode;

        private InsertRequestBuilder() {
            super();
            this.continueOnError = false;
            this.tokenMode = TokenMode.DISABLE;
        }

        @Override
        public InsertRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        @Override
        public InsertRequestBuilder values(ArrayList<HashMap<String, Object>> values) {
            super.values(values);
            return this;
        }

        @Override
        public InsertRequestBuilder tokens(ArrayList<HashMap<String, Object>> tokens) {
            super.tokens(tokens);
            return this;
        }

        @Override
        public InsertRequestBuilder returnTokens(Boolean returnTokens) {
            super.returnTokens(returnTokens);
            return this;
        }

        @Override
        public InsertRequestBuilder upsert(String upsert) {
            super.upsert(upsert);
            return this;
        }

        public InsertRequestBuilder homogeneous(Boolean homogeneous) {
            this.homogeneous = homogeneous;
            return this;
        }

        public InsertRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError != null && continueOnError;
            return this;
        }

        public InsertRequestBuilder tokenMode(TokenMode tokenMode) {
            this.tokenMode = tokenMode == null ? TokenMode.DISABLE : tokenMode;
            return this;
        }

        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
