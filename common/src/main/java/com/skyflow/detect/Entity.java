package com.skyflow.detect;

/**
 * Configures how a single entity type is detected and de-identified.
 */
public final class Entity {
    private final EntityType entityType;
    private final DeidentificationType deidentificationType;
    private final String destination;
    private final Transformation transformation;

    private Entity(EntityBuilder builder) {
        this.entityType = builder.entityType;
        this.deidentificationType = builder.deidentificationType;
        this.destination = builder.destination;
        this.transformation = builder.transformation;
    }

    /**
     * @return a new builder
     */
    public static EntityBuilder builder() {
        return new EntityBuilder();
    }

    /**
     * @return Entity type this rule applies to. Defaults to ALL on the server.
     */
    public EntityType getEntityType() {
        return entityType;
    }

    /**
     * @return Replacement strategy. Defaults to ENTITY_UNIQUE_COUNTER on the server.
     */
    public DeidentificationType getDeidentificationType() {
        return deidentificationType;
    }

    /**
     * @return Where vault tokens are stored: a table.column (skyvault) or token-group name (flowvault). Only used with VAULT_TOKEN.
     */
    public String getDestination() {
        return destination;
    }

    /**
     * @return Optional transformation applied to the detected value.
     */
    public Transformation getTransformation() {
        return transformation;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "entityType=" + entityType +
                ", deidentificationType=" + deidentificationType +
                ", destination=" + destination +
                ", transformation=" + transformation +
                '}';
    }

    public static final class EntityBuilder {
        private EntityType entityType;
        private DeidentificationType deidentificationType;
        private String destination;
        private Transformation transformation;

        private EntityBuilder() {
        }

        /**
         * @param entityType Entity type this rule applies to. Defaults to ALL on the server.
         * @return this builder
         */
        public EntityBuilder entityType(EntityType entityType) {
            this.entityType = entityType;
            return this;
        }

        /**
         * @param deidentificationType Replacement strategy. Defaults to ENTITY_UNIQUE_COUNTER on the server.
         * @return this builder
         */
        public EntityBuilder deidentificationType(DeidentificationType deidentificationType) {
            this.deidentificationType = deidentificationType;
            return this;
        }

        /**
         * @param destination Where vault tokens are stored: a table.column (skyvault) or token-group name (flowvault). Only used with VAULT_TOKEN.
         * @return this builder
         */
        public EntityBuilder destination(String destination) {
            this.destination = destination;
            return this;
        }

        /**
         * @param transformation Optional transformation applied to the detected value.
         * @return this builder
         */
        public EntityBuilder transformation(Transformation transformation) {
            this.transformation = transformation;
            return this;
        }

        public Entity build() {
            return new Entity(this);
        }
    }
}
