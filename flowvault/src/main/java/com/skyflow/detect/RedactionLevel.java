package com.skyflow.detect;

/**
 * Controls how one group of tokens is rendered when re-identifying on a flowvault vault: the {@code tokenGroupName}
 * the rule applies to and the named {@code redactionPattern}, configured in the vault, to render its values with.
 * Element of {@code redactionLevel} on {@code ReidentifyStringRequest} and {@code ReidentifyFileRequest}. Both
 * fields are optional on the API and are sent as given.
 */
public final class RedactionLevel extends RedactionRule {
    private final String tokenGroupName;
    private final String redactionPattern;

    private RedactionLevel(RedactionLevelBuilder builder) {
        this.tokenGroupName = builder.tokenGroupName;
        this.redactionPattern = builder.redactionPattern;
    }

    /**
     * @return a new builder
     */
    public static RedactionLevelBuilder builder() {
        return new RedactionLevelBuilder();
    }

    /**
     * @return Token group the rule applies to.
     */
    public String getTokenGroupName() {
        return tokenGroupName;
    }

    /**
     * @return Name of the redaction pattern, configured in the vault, applied to that group's values.
     */
    public String getRedactionPattern() {
        return redactionPattern;
    }

    @Override
    String apiEntityName() {
        return null;
    }

    @Override
    String apiRedactionType() {
        return null;
    }

    @Override
    String apiTokenGroupName() {
        return tokenGroupName;
    }

    @Override
    String apiRedactionPattern() {
        return redactionPattern;
    }

    @Override
    public String toString() {
        return "RedactionLevel{" +
                "tokenGroupName=" + tokenGroupName +
                ", redactionPattern=" + redactionPattern +
                '}';
    }

    public static final class RedactionLevelBuilder {
        private String tokenGroupName;
        private String redactionPattern;

        private RedactionLevelBuilder() {
        }

        /**
         * @param tokenGroupName Token group the rule applies to.
         * @return this builder
         */
        public RedactionLevelBuilder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        /**
         * @param redactionPattern Name of a redaction pattern configured in the vault.
         * @return this builder
         */
        public RedactionLevelBuilder redactionPattern(String redactionPattern) {
            this.redactionPattern = redactionPattern;
            return this;
        }

        public RedactionLevel build() {
            return new RedactionLevel(this);
        }
    }
}
