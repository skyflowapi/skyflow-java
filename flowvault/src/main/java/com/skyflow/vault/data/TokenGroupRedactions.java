package com.skyflow.vault.data;

public class TokenGroupRedactions {
    private final TokenGroupRedactionsBuilder builder;

    private TokenGroupRedactions(TokenGroupRedactionsBuilder builder) {
        this.builder = builder;
    }
    public String getTokenGroupName() {
        return this.builder.tokenGroupName;
    }

    public String getRedaction() {
        return this.builder.redaction;
    }

    public static TokenGroupRedactionsBuilder builder() {
        return new TokenGroupRedactionsBuilder();
    }

    public static final class TokenGroupRedactionsBuilder {
        private String tokenGroupName;
        private String redaction;

        public TokenGroupRedactionsBuilder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        public TokenGroupRedactionsBuilder redaction(String redaction) {
            this.redaction = redaction;
            return this;
        }

        public TokenGroupRedactions build() {
            return new TokenGroupRedactions(this);
        }
    }
}