package com.skyflow.detect;

import java.util.List;

/**
 * A custom, regex-defined entity type.
 */
public final class CustomType {
    private final String label;
    private final List<String> match;

    private CustomType(CustomTypeBuilder builder) {
        this.label = builder.label;
        this.match = builder.match;
    }

    /**
     * @return a new builder
     */
    public static CustomTypeBuilder builder() {
        return new CustomTypeBuilder();
    }

    /**
     * @return Label reported as the entityType of matches.
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

    @Override
    public String toString() {
        return "CustomType{" +
                "label=" + label +
                ", match=" + match +
                '}';
    }

    public static final class CustomTypeBuilder {
        private String label;
        private List<String> match;

        private CustomTypeBuilder() {
        }

        /**
         * @param label Label reported as the entityType of matches.
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

        public CustomType build() {
            return new CustomType(this);
        }
    }
}
