package com.skyflow.detect;

import java.util.List;

/**
 * Request for {@code reidentifyString}.
 */
public final class ReidentifyStringRequest {
    private final String text;
    private final List<RedactionLevel> redactionLevel;

    private ReidentifyStringRequest(ReidentifyStringRequestBuilder builder) {
        this.text = builder.text;
        this.redactionLevel = builder.redactionLevel;
    }

    /**
     * @return a new builder
     */
    public static ReidentifyStringRequestBuilder builder() {
        return new ReidentifyStringRequestBuilder();
    }

    /**
     * @return Text containing tokens to re-identify. Required.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Optional per-group rendering rules.
     */
    public List<RedactionLevel> getRedactionLevel() {
        return redactionLevel;
    }

    @Override
    public String toString() {
        return "ReidentifyStringRequest{" +
                "text=" + text +
                ", redactionLevel=" + redactionLevel +
                '}';
    }

    public static final class ReidentifyStringRequestBuilder {
        private String text;
        private List<RedactionLevel> redactionLevel;

        private ReidentifyStringRequestBuilder() {
        }

        /**
         * @param text Text containing tokens to re-identify. Required.
         * @return this builder
         */
        public ReidentifyStringRequestBuilder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * @param redactionLevel Optional per-group rendering rules.
         * @return this builder
         */
        public ReidentifyStringRequestBuilder redactionLevel(List<RedactionLevel> redactionLevel) {
            this.redactionLevel = redactionLevel;
            return this;
        }

        public ReidentifyStringRequest build() {
            return new ReidentifyStringRequest(this);
        }
    }
}
