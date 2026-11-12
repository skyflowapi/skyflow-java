package com.skyflow.vault.detect;

import java.util.List;

/**
 * A custom, regex-defined entity type. Matches are de-identified like any other entity: the label is
 * reported as their entity type and, with VAULT_TOKEN, becomes the default token prefix.
 */
public final class CustomType {
    private final String label;
    private final List<String> match;
    private final DeidentificationType deidentificationType;
    private final String destination;
    private final IncludePrefix includePrefix;
    private final String prefixOverride;

    private CustomType(CustomTypeBuilder builder) {
        this.label = builder.label;
        this.match = builder.match;
        this.deidentificationType = builder.deidentificationType;
        this.destination = builder.destination;
        this.includePrefix = builder.includePrefix;
        this.prefixOverride = builder.prefixOverride;
    }

    /**
     * @return a new builder
     */
    public static CustomTypeBuilder builder() {
        return new CustomTypeBuilder();
    }

    /**
     * @return Label reported as the entityType of matches, and the default token prefix.
     */
    public String getLabel() {
        return label;
    }

    /**
     * @return Regular expressions that identify the custom entity.
     */
    public List<String> getMatch() {
        return match;
    }

    /**
     * @return Replacement strategy for matches of this type.
     */
    public DeidentificationType getDeidentificationType() {
        return deidentificationType;
    }

    /**
     * @return Where vault tokens are stored: a table.column (skyvault) or token-group name (flowvault). Required with VAULT_TOKEN.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return Whether the label is included as a prefix in the generated token. Defaults to YES on the server. Only used with VAULT_TOKEN.
     */
    public IncludePrefix getIncludePrefix() {
        return includePrefix;
    }

    /**
     * @return Replacement for the label as the prefix in the generated token. Not valid when includePrefix is NO.
     */
    public String getPrefixOverride() {
        return prefixOverride;
    }

    @Override
    public String toString() {
        return "CustomType{" +
                "label=" + label +
                ", match=" + match +
                ", deidentificationType=" + deidentificationType +
                ", destination=" + destination +
                ", includePrefix=" + includePrefix +
                ", prefixOverride=" + prefixOverride +
                '}';
    }

    public static final class CustomTypeBuilder {
        private String label;
        private List<String> match;
        private DeidentificationType deidentificationType;
        private String destination;
        private IncludePrefix includePrefix;
        private String prefixOverride;

        private CustomTypeBuilder() {
        }

        /**
         * @param label Label reported as the entityType of matches, and the default token prefix.
         * @return this builder
         */
        public CustomTypeBuilder label(String label) {
            this.label = label;
            return this;
        }

        /**
         * @param match Regular expressions that identify the custom entity.
         * @return this builder
         */
        public CustomTypeBuilder match(List<String> match) {
            this.match = match;
            return this;
        }

        /**
         * @param deidentificationType Replacement strategy for matches of this type.
         * @return this builder
         */
        public CustomTypeBuilder deidentificationType(DeidentificationType deidentificationType) {
            this.deidentificationType = deidentificationType;
            return this;
        }

        /**
         * @param destination Where vault tokens are stored: a table.column (skyvault) or token-group name (flowvault). Required with VAULT_TOKEN.
         * @return this builder
         */
        public CustomTypeBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        /**
         * @param includePrefix Whether the label is included as a prefix in the generated token. Defaults to YES on the server. Only used with VAULT_TOKEN. NO returns the raw vault token, which cannot be re-identified later.
         * @return this builder
         */
        public CustomTypeBuilder includePrefix(IncludePrefix includePrefix) {
            this.includePrefix = includePrefix;
            return this;
        }

        /**
         * @param prefixOverride Replacement for the label as the prefix in the generated token. Not valid when includePrefix is NO.
         * @return this builder
         */
        public CustomTypeBuilder prefixOverride(String prefixOverride) {
            this.prefixOverride = prefixOverride;
            return this;
        }

        public CustomType build() {
            return new CustomType(this);
        }
    }
}
