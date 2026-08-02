package com.skyflow.vault.detect;


import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.TokenType;

import java.util.List;


public class TokenFormat {
    private final TokenFormatBuilder builder;

    private TokenFormat(TokenFormatBuilder builder) {
        this.builder = builder;
    }

    public static TokenFormatBuilder builder() {
        return new TokenFormatBuilder();
    }

    public TokenType getDefault() {
        return this.builder.defaultType;
    }

    public List<DetectEntities> getVaultToken() {
        return this.builder.vaultToken;
    }

    public List<DetectEntities> getEntityUniqueCounter() {
        return this.builder.entityUniqueCounter;
    }

    public List<DetectEntities> getEntityOnly() {
        return this.builder.entityOnly;
    }

    public static final class TokenFormatBuilder {
        private TokenType defaultType;
        private List<DetectEntities> vaultToken;
        private List<DetectEntities> entityUniqueCounter;
        private List<DetectEntities> entityOnly;

        private TokenFormatBuilder() {
            this.defaultType = TokenType.ENTITY_UNIQUE_COUNTER;
        }

        public TokenFormatBuilder defaultType(TokenType defaultType) {
            this.defaultType = defaultType != null ? defaultType : TokenType.ENTITY_UNIQUE_COUNTER;
            return this;
        }

        public TokenFormatBuilder vaultToken(List<DetectEntities> vaultToken) {
            this.vaultToken = vaultToken;
            return this;
        }

        public TokenFormatBuilder entityUniqueCounter(List<DetectEntities> entityUniqueCounter) {
            this.entityUniqueCounter = entityUniqueCounter;
            return this;
        }

        public TokenFormatBuilder entityOnly(List<DetectEntities> entityOnly) {
            this.entityOnly = entityOnly;
            return this;
        }

        public TokenFormat build() {
            return new TokenFormat(this);
        }
    }
}