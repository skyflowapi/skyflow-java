package com.skyflow.detect;

import java.util.List;

/**
 * Detection policy: which entities to look for and how to replace them.
 */
public final class Detect {
    private final List<Entity> entities;
    private final List<ObjectEntity> objectEntities;
    private final List<String> restrict;
    private final List<String> skip;
    private final List<CustomType> customTypes;
    private final ReturnEntitiesType returnEntities;

    private Detect(DetectBuilder builder) {
        this.entities = builder.entities;
        this.objectEntities = builder.objectEntities;
        this.restrict = builder.restrict;
        this.skip = builder.skip;
        this.customTypes = builder.customTypes;
        this.returnEntities = builder.returnEntities;
    }

    /**
     * @return a new builder
     */
    public static DetectBuilder builder() {
        return new DetectBuilder();
    }

    /**
     * @return Per-entity rules.
     */
    public List<Entity> getEntities() {
        return entities;
    }

    /**
     * @return Per-object rules. File inputs only.
     */
    public List<ObjectEntity> getObjectEntities() {
        return objectEntities;
    }

    /**
     * @return Regular expressions that are always treated as sensitive.
     */
    public List<String> getRestrict() {
        return restrict;
    }

    /**
     * @return Regular expressions that are never treated as sensitive.
     */
    public List<String> getSkip() {
        return skip;
    }

    /**
     * @return Custom regex-defined entity types.
     */
    public List<CustomType> getCustomTypes() {
        return customTypes;
    }

    /**
     * @return Which entities to return. Defaults to EXCLUDE_SENSITIVE_DATA on the server.
     */
    public ReturnEntitiesType getReturnEntities() {
        return returnEntities;
    }

    @Override
    public String toString() {
        return "Detect{" +
                "entities=" + entities +
                ", objectEntities=" + objectEntities +
                ", restrict=" + restrict +
                ", skip=" + skip +
                ", customTypes=" + customTypes +
                ", returnEntities=" + returnEntities +
                '}';
    }

    public static final class DetectBuilder {
        private List<Entity> entities;
        private List<ObjectEntity> objectEntities;
        private List<String> restrict;
        private List<String> skip;
        private List<CustomType> customTypes;
        private ReturnEntitiesType returnEntities;

        private DetectBuilder() {
        }

        /**
         * @param entities Per-entity rules.
         * @return this builder
         */
        public DetectBuilder entities(List<Entity> entities) {
            this.entities = entities;
            return this;
        }

        /**
         * @param objectEntities Per-object rules. File inputs only.
         * @return this builder
         */
        public DetectBuilder objectEntities(List<ObjectEntity> objectEntities) {
            this.objectEntities = objectEntities;
            return this;
        }

        /**
         * @param restrict Regular expressions that are always treated as sensitive.
         * @return this builder
         */
        public DetectBuilder restrict(List<String> restrict) {
            this.restrict = restrict;
            return this;
        }

        /**
         * @param skip Regular expressions that are never treated as sensitive.
         * @return this builder
         */
        public DetectBuilder skip(List<String> skip) {
            this.skip = skip;
            return this;
        }

        /**
         * @param customTypes Custom regex-defined entity types.
         * @return this builder
         */
        public DetectBuilder customTypes(List<CustomType> customTypes) {
            this.customTypes = customTypes;
            return this;
        }

        /**
         * @param returnEntities Which entities to return. Defaults to EXCLUDE_SENSITIVE_DATA on the server.
         * @return this builder
         */
        public DetectBuilder returnEntities(ReturnEntitiesType returnEntities) {
            this.returnEntities = returnEntities;
            return this;
        }

        public Detect build() {
            return new Detect(this);
        }
    }
}
