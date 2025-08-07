package com.skyflow.v2.vault.data;

import com.skyflow.v2.enums.TokenMode;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertRequest {
    private final InsertRequestBuilder builder;

    private InsertRequest(InsertRequestBuilder builder) {
        this.builder = builder;
    }

    public static InsertRequestBuilder builder() {
        return new InsertRequestBuilder();
    }

    public String getTable() {
        return this.builder.table;
    }

    public ArrayList<HashMap<String, Object>> getValues() {
        return this.builder.values;
    }

    public ArrayList<HashMap<String, Object>> getTokens() {
        return this.builder.tokens;
    }

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

    public String getUpsert() {
        return this.builder.upsert;
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

    public static final class InsertRequestBuilder {
        private String table;
        private ArrayList<HashMap<String, Object>> values;
        private ArrayList<HashMap<String, Object>> tokens;
        private Boolean returnTokens;
        private String upsert;
        private Boolean homogeneous;
        private Boolean continueOnError;
        private TokenMode tokenMode;

        private InsertRequestBuilder() {
            this.returnTokens = false;
            this.continueOnError = false;
            this.tokenMode = TokenMode.DISABLE;
        }

        public InsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public InsertRequestBuilder values(ArrayList<HashMap<String, Object>> values) {
            this.values = values;
            return this;
        }

        public InsertRequestBuilder tokens(ArrayList<HashMap<String, Object>> tokens) {
            this.tokens = tokens;
            return this;
        }

        public InsertRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens != null && returnTokens;
            return this;
        }

        public InsertRequestBuilder upsert(String upsert) {
            this.upsert = upsert;
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
