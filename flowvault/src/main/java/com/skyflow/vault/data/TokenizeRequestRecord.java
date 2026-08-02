package com.skyflow.vault.data;

import java.util.List;

public class TokenizeRequestRecord {
    private final Object value;
    private final Object token;
    private final List<String> tokenGroupNames;

    protected TokenizeRequestRecord(TokenizeRequestRecordBuilder builder) {
        this.value = builder.value;
        this.token = builder.token;
        this.tokenGroupNames = builder.tokenGroupNames;
    }

    public static TokenizeRequestRecordBuilder builder() {
        return new TokenizeRequestRecordBuilder();
    }

    public Object getValue() {
        return this.value;
    }

    /** Bring-your-own-token value, when the caller supplies the token instead of generating one. */
    public Object getToken() {
        return this.token;
    }

    public List<String> getTokenGroupNames() {
        return this.tokenGroupNames;
    }

    public static class TokenizeRequestRecordBuilder {
        protected Object value;
        protected Object token;
        protected List<String> tokenGroupNames;

        protected TokenizeRequestRecordBuilder() {}

        public TokenizeRequestRecordBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public TokenizeRequestRecordBuilder token(Object token) {
            this.token = token;
            return this;
        }

        public TokenizeRequestRecordBuilder tokenGroupNames(List<String> tokenGroupNames) {
            this.tokenGroupNames = tokenGroupNames;
            return this;
        }

        public TokenizeRequestRecord build() {
            return new TokenizeRequestRecord(this);
        }
    }
}
