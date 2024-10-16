package com.skyflow.vault.data;

import com.skyflow.enums.Byot;

import java.util.HashMap;

public class UpdateRequest {
    private final String table;
    private final String id;
    private final Boolean returnTokens;
    private final HashMap<String, Object> values;
    private final HashMap<String, Object> tokens;
    private final Byot tokenStrict;

    private UpdateRequest(UpdateRequestBuilder builder) {
        this.table = builder.table;
        this.id = builder.id;
        this.returnTokens = builder.returnTokens;
        this.values = builder.values;
        this.tokens = builder.tokens;
        this.tokenStrict = builder.tokenStrict;
    }

    public static UpdateRequestBuilder builder() {
        return new UpdateRequestBuilder();
    }

    public String getTable() {
        return table;
    }

    public String getId() {
        return id;
    }

    public Boolean getReturnTokens() {
        return returnTokens;
    }

    public HashMap<String, Object> getValues() {
        return values;
    }

    public HashMap<String, Object> getTokens() {
        return tokens;
    }

    public Byot getTokenStrict() {
        return tokenStrict;
    }

    public static class UpdateRequestBuilder {
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
