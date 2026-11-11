package com.skyflow.detect;

import java.util.List;

/**
 * Request for {@code checkGuardrails}: screens text for toxicity and denied topics.
 */
public final class CheckGuardrailsRequest {
    private final String text;
    private final Boolean checkToxicity;
    private final List<String> denyTopics;

    private CheckGuardrailsRequest(CheckGuardrailsRequestBuilder builder) {
        this.text = builder.text;
        this.checkToxicity = builder.checkToxicity;
        this.denyTopics = builder.denyTopics;
    }

    /**
     * @return a new builder
     */
    public static CheckGuardrailsRequestBuilder builder() {
        return new CheckGuardrailsRequestBuilder();
    }

    /**
     * @return Text to screen. Required.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Whether to check the text for toxicity.
     */
    public Boolean getCheckToxicity() {
        return checkToxicity;
    }

    /**
     * @return Topics the text must not discuss.
     */
    public List<String> getDenyTopics() {
        return denyTopics;
    }

    @Override
    public String toString() {
        return "CheckGuardrailsRequest{" +
                "text=" + text +
                ", checkToxicity=" + checkToxicity +
                ", denyTopics=" + denyTopics +
                '}';
    }

    public static final class CheckGuardrailsRequestBuilder {
        private String text;
        private Boolean checkToxicity;
        private List<String> denyTopics;

        private CheckGuardrailsRequestBuilder() {
        }

        /**
         * @param text Text to screen. Required.
         * @return this builder
         */
        public CheckGuardrailsRequestBuilder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * @param checkToxicity Whether to check the text for toxicity.
         * @return this builder
         */
        public CheckGuardrailsRequestBuilder checkToxicity(Boolean checkToxicity) {
            this.checkToxicity = checkToxicity;
            return this;
        }

        /**
         * @param denyTopics Topics the text must not discuss.
         * @return this builder
         */
        public CheckGuardrailsRequestBuilder denyTopics(List<String> denyTopics) {
            this.denyTopics = denyTopics;
            return this;
        }

        public CheckGuardrailsRequest build() {
            return new CheckGuardrailsRequest(this);
        }
    }
}
