package com.skyflow.vault.data;

import com.skyflow.enums.Byot;

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

    public Boolean getTokenMode() {
        return this.builder.tokenMode;
    }

    public Boolean getContinueOnError() {
        return this.builder.continueOnError;
    }

    public Byot getTokenStrict() {
        return this.builder.tokenStrict;
    }

    public static final class InsertRequestBuilder {
        private String table;
        private ArrayList<HashMap<String, Object>> values;
        private ArrayList<HashMap<String, Object>> tokens;
        private Boolean returnTokens;
        private String upsert;
        private Boolean homogeneous;
        private Boolean tokenMode;
        private Boolean continueOnError;
        private Byot tokenStrict;

        private InsertRequestBuilder() {
            this.returnTokens = true;
            this.tokenMode = false;
            this.continueOnError = false;
            this.tokenStrict = Byot.DISABLE;
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
            this.returnTokens = returnTokens == null || returnTokens;
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

        public InsertRequestBuilder tokenMode(Boolean tokenMode) {
            this.tokenMode = tokenMode != null && tokenMode;
            return this;
        }

        public InsertRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError != null && continueOnError;
            return this;
        }

        public InsertRequestBuilder tokenStrict(Byot tokenStrict) {
            this.tokenStrict = tokenStrict == null ? Byot.DISABLE : tokenStrict;
            return this;
        }

        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
