package com.skyflow.detect;

/**
 * Request for {@code deidentifyString}. configurationId and configuration are both optional and are sent
 * exactly as supplied; the server applies its own rules for their combination.
 */
public final class DeidentifyStringRequest {
    private final String text;
    private final String configurationId;
    private final DetectConfiguration configuration;

    private DeidentifyStringRequest(DeidentifyStringRequestBuilder builder) {
        this.text = builder.text;
        this.configurationId = builder.configurationId;
        this.configuration = builder.configuration;
    }

    /**
     * @return a new builder
     */
    public static DeidentifyStringRequestBuilder builder() {
        return new DeidentifyStringRequestBuilder();
    }

    /**
     * @return Text to de-identify. Required.
     */
    public String getText() {
        return text;
    }

    /**
     * @return Id of a stored Detect configuration.
     */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
     * @return Inline Detect configuration.
     */
    public DetectConfiguration getConfiguration() {
        return configuration;
    }

    @Override
    public String toString() {
        return "DeidentifyStringRequest{" +
                "text=" + text +
                ", configurationId=" + configurationId +
                ", configuration=" + configuration +
                '}';
    }

    public static final class DeidentifyStringRequestBuilder {
        private String text;
        private String configurationId;
        private DetectConfiguration configuration;

        private DeidentifyStringRequestBuilder() {
        }

        /**
         * @param text Text to de-identify. Required.
         * @return this builder
         */
        public DeidentifyStringRequestBuilder text(String text) {
            this.text = text;
            return this;
        }

        /**
         * @param configurationId Id of a stored Detect configuration.
         * @return this builder
         */
        public DeidentifyStringRequestBuilder configurationId(String configurationId) {
            this.configurationId = configurationId;
            return this;
        }

        /**
         * @param configuration Inline Detect configuration.
         * @return this builder
         */
        public DeidentifyStringRequestBuilder configuration(DetectConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        public DeidentifyStringRequest build() {
            return new DeidentifyStringRequest(this);
        }
    }
}
