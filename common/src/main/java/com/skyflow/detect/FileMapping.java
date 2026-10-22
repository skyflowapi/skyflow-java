package com.skyflow.detect;

/**
 * Maps an input file location to output locations. File inputs only.
 */
public final class FileMapping {
    private final String source;
    private final String destination;
    private final String outputFileNameSuffix;
    private final String entitiesDestination;
    private final String objectEntitiesDestination;

    private FileMapping(FileMappingBuilder builder) {
        this.source = builder.source;
        this.destination = builder.destination;
        this.outputFileNameSuffix = builder.outputFileNameSuffix;
        this.entitiesDestination = builder.entitiesDestination;
        this.objectEntitiesDestination = builder.objectEntitiesDestination;
    }

    /**
     * @return a new builder
     */
    public static FileMappingBuilder builder() {
        return new FileMappingBuilder();
    }

    /**
     * @return Source location.
     */
    public String getSource() {
        return source;
    }

    /**
     * @return Destination for the processed file.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return Suffix appended to the processed file name.
     */
    public String getOutputFileNameSuffix() {
        return outputFileNameSuffix;
    }

    /**
     * @return Destination for the detected entities file.
     */
    public String getEntitiesDestination() {
        return entitiesDestination;
    }

    /**
     * @return Destination for the detected object entities file.
     */
    public String getObjectEntitiesDestination() {
        return objectEntitiesDestination;
    }

    @Override
    public String toString() {
        return "FileMapping{" +
                "source=" + source +
                ", destination=" + destination +
                ", outputFileNameSuffix=" + outputFileNameSuffix +
                ", entitiesDestination=" + entitiesDestination +
                ", objectEntitiesDestination=" + objectEntitiesDestination +
                '}';
    }

    public static final class FileMappingBuilder {
        private String source;
        private String destination;
        private String outputFileNameSuffix;
        private String entitiesDestination;
        private String objectEntitiesDestination;

        private FileMappingBuilder() {
        }

        /**
         * @param source Source location.
         * @return this builder
         */
        public FileMappingBuilder source(String source) {
            this.source = source;
            return this;
        }

        /**
         * @param destination Destination for the processed file.
         * @return this builder
         */
        public FileMappingBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        /**
         * @param outputFileNameSuffix Suffix appended to the processed file name.
         * @return this builder
         */
        public FileMappingBuilder outputFileNameSuffix(String outputFileNameSuffix) {
            this.outputFileNameSuffix = outputFileNameSuffix;
            return this;
        }

        /**
         * @param entitiesDestination Destination for the detected entities file.
         * @return this builder
         */
        public FileMappingBuilder entitiesDestination(String entitiesDestination) {
            this.entitiesDestination = entitiesDestination;
            return this;
        }

        /**
         * @param objectEntitiesDestination Destination for the detected object entities file.
         * @return this builder
         */
        public FileMappingBuilder objectEntitiesDestination(String objectEntitiesDestination) {
            this.objectEntitiesDestination = objectEntitiesDestination;
            return this;
        }

        public FileMapping build() {
            return new FileMapping(this);
        }
    }
}
