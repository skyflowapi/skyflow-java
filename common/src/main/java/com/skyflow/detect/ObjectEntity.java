package com.skyflow.detect;

/**
 * Configures detection of a visual object type in images or video.
 */
public final class ObjectEntity {
    private final ObjectEntityType entityType;
    private final ObjectDeidentificationType deidentificationType;

    private ObjectEntity(ObjectEntityBuilder builder) {
        this.entityType = builder.entityType;
        this.deidentificationType = builder.deidentificationType;
    }

    /**
     * @return a new builder
     */
    public static ObjectEntityBuilder builder() {
        return new ObjectEntityBuilder();
    }

    /**
     * @return Object type this rule applies to.
     */
    public ObjectEntityType getEntityType() {
        return entityType;
    }

    /**
     * @return How matches are handled.
     */
    public ObjectDeidentificationType getDeidentificationType() {
        return deidentificationType;
    }

    @Override
    public String toString() {
        return "ObjectEntity{" +
                "entityType=" + entityType +
                ", deidentificationType=" + deidentificationType +
                '}';
    }

    public static final class ObjectEntityBuilder {
        private ObjectEntityType entityType;
        private ObjectDeidentificationType deidentificationType;

        private ObjectEntityBuilder() {
        }

        /**
         * @param entityType Object type this rule applies to.
         * @return this builder
         */
        public ObjectEntityBuilder entityType(ObjectEntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * @param deidentificationType How matches are handled.
         * @return this builder
         */
        public ObjectEntityBuilder deidentificationType(ObjectDeidentificationType deidentificationType) {
            this.deidentificationType = deidentificationType;
            return this;
        }

        public ObjectEntity build() {
            return new ObjectEntity(this);
        }
    }
}
