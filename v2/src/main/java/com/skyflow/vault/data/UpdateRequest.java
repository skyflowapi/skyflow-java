package com.skyflow.vault.data;

import com.skyflow.enums.Byot;

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

    public String getId() {
        return this.builder.id;
    }

    public Boolean getReturnTokens() {
        return this.builder.returnTokens;
    }

    public HashMap<String, Object> getValues() {
        return this.builder.values;
    }

    public HashMap<String, Object> getTokens() {
        return this.builder.tokens;
    }

    public Byot getTokenStrict() {
        return this.builder.tokenStrict;
    }

    public static final class UpdateRequestBuilder {
        private String table;
        private String id;
        private Boolean returnTokens;
        private HashMap<String, Object> values;
        private HashMap<String, Object> tokens;
        private Byot tokenStrict;

        private UpdateRequestBuilder() {
            this.returnTokens = true;
            this.tokenStrict = Byot.DISABLE;
        }

        public UpdateRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public UpdateRequestBuilder id(String id) {
            this.id = id;
            return this;
        }

        public UpdateRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens;
            return this;
        }

        public UpdateRequestBuilder values(HashMap<String, Object> values) {
            this.values = values;
            return this;
        }

        public UpdateRequestBuilder tokens(HashMap<String, Object> tokens) {
            this.tokens = tokens;
            return this;
        }

        public UpdateRequestBuilder tokenStrict(Byot tokenStrict) {
            this.tokenStrict = tokenStrict;
            return this;
        }

        public UpdateRequest build() {
            return new UpdateRequest(this);
        }
    }
}
