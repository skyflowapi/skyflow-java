package com.skyflow.vault.data;

import java.util.List;

public class TokenizeRecord {
    private final TokenizeRecordBuilder builder;

    private TokenizeRecord(TokenizeRecordBuilder builder) {
        this.builder = builder;
    }

    public static TokenizeRecordBuilder builder() {
        return new TokenizeRecordBuilder();
    }

    public Object getValue() {
        return this.builder.value;
    }

    public List<String> getTokenGroupNames() {
        return this.builder.tokenGroupNames;
    }

    public static final class TokenizeRecordBuilder {
        private Object value;
        private List<String> tokenGroupNames;

        private TokenizeRecordBuilder() {}

        public TokenizeRecordBuilder value(Object value) {
            this.value = value;
            return this;
        }

        public TokenizeRecordBuilder tokenGroupNames(List<String> tokenGroupNames) {
            this.tokenGroupNames = tokenGroupNames;
            return this;
        }

        public TokenizeRecord build() {
            return new TokenizeRecord(this);
        }
    }
}
