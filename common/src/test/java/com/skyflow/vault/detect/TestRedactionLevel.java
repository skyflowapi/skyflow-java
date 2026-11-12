package com.skyflow.vault.detect;

import com.skyflow.enums.RedactionType;

/** Test stand-in for the per-SDK {@code RedactionLevel} classes, which common cannot see. Carries all four API fields. */
public final class TestRedactionLevel extends RedactionRule {
    private final EntityType entityName;
    private final RedactionType redactionType;
    private final String tokenGroupName;
    private final String redactionPattern;

    private TestRedactionLevel(Builder b) {
        this.entityName = b.entityName;
        this.redactionType = b.redactionType;
        this.tokenGroupName = b.tokenGroupName;
        this.redactionPattern = b.redactionPattern;
    }

    public static Builder builder() {
        return new Builder();
    }

    public EntityType getEntityName() {
        return entityName;
    }

    public RedactionType getRedactionType() {
        return redactionType;
    }

    public String getTokenGroupName() {
        return tokenGroupName;
    }

    public String getRedactionPattern() {
        return redactionPattern;
    }

    @Override
    String apiEntityName() {
        return entityName == null ? null : entityName.name();
    }

    @Override
    String apiRedactionType() {
        return redactionType == null ? null : redactionType.name();
    }

    @Override
    String apiTokenGroupName() {
        return tokenGroupName;
    }

    @Override
    String apiRedactionPattern() {
        return redactionPattern;
    }

    public static final class Builder {
        private EntityType entityName;
        private RedactionType redactionType;
        private String tokenGroupName;
        private String redactionPattern;

        public Builder entityName(EntityType entityName) {
            this.entityName = entityName;
            return this;
        }

        public Builder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        public Builder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        public Builder redactionPattern(String redactionPattern) {
            this.redactionPattern = redactionPattern;
            return this;
        }

        public TestRedactionLevel build() {
            return new TestRedactionLevel(this);
        }
    }
}
