package com.skyflow.detect;

import java.util.List;

/**
 * An inline Detect configuration, used instead of a stored configuration id.
 */
public final class DetectConfiguration {
    private final String name;
    private final String description;
    private final Detect detect;
    private final List<FileMapping> fileMapping;
    private final Media media;

    private DetectConfiguration(DetectConfigurationBuilder builder) {
        this.name = builder.name;
        this.description = builder.description;
        this.detect = builder.detect;
        this.fileMapping = builder.fileMapping;
        this.media = builder.media;
    }

    /**
     * @return a new builder
     */
    public static DetectConfigurationBuilder builder() {
        return new DetectConfigurationBuilder();
    }

    /**
     * @return Configuration name.
     */
    public String getName() {
        return name;
    }

    /**
     * @return Configuration description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return Detection policy.
     */
    public Detect getDetect() {
        return detect;
    }

    /**
     * @return File mappings. File inputs only.
     */
    public List<FileMapping> getFileMapping() {
        return fileMapping;
    }

    /**
     * @return Media options. File inputs only.
     */
    public Media getMedia() {
        return media;
    }

    @Override
    public String toString() {
        return "DetectConfiguration{" +
                "name=" + name +
                ", description=" + description +
                ", detect=" + detect +
                ", fileMapping=" + fileMapping +
                ", media=" + media +
                '}';
    }

    public static final class DetectConfigurationBuilder {
        private String name;
        private String description;
        private Detect detect;
        private List<FileMapping> fileMapping;
        private Media media;

        private DetectConfigurationBuilder() {
        }

        /**
         * @param name Configuration name.
         * @return this builder
         */
        public DetectConfigurationBuilder name(String name) {
            this.name = name;
            return this;
        }

        /**
         * @param description Configuration description.
         * @return this builder
         */
        public DetectConfigurationBuilder description(String description) {
            this.description = description;
            return this;
        }

        /**
         * @param detect Detection policy.
         * @return this builder
         */
        public DetectConfigurationBuilder detect(Detect detect) {
            this.detect = detect;
            return this;
        }

        /**
         * @param fileMapping File mappings. File inputs only.
         * @return this builder
         */
        public DetectConfigurationBuilder fileMapping(List<FileMapping> fileMapping) {
            this.fileMapping = fileMapping;
            return this;
        }

        /**
         * @param media Media options. File inputs only.
         * @return this builder
         */
        public DetectConfigurationBuilder media(Media media) {
            this.media = media;
            return this;
        }

        public DetectConfiguration build() {
            return new DetectConfiguration(this);
        }
    }
}
