package com.skyflow.detect;

/**
 * Configures how a single entity type is detected and de-identified.
 */
public final class Entity {
    private final EntityType entityType;
    private final DeidentificationType deidentificationType;
    private final String destination;
    private final Transformation transformation;
    private final IncludePrefix includePrefix;
    private final String prefixOverride;

    private Entity(EntityBuilder builder) {
        this.entityType = builder.entityType;
        this.deidentificationType = builder.deidentificationType;
        this.destination = builder.destination;
        this.transformation = builder.transformation;
        this.includePrefix = builder.includePrefix;
        this.prefixOverride = builder.prefixOverride;
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

    /**
     * @return Whether the entity label is included as a prefix in the generated token. Defaults to YES on the server. Only used with VAULT_TOKEN.
     */
    public IncludePrefix getIncludePrefix() {
        return includePrefix;
    }

    /**
     * @return Replacement for the default entity-type prefix in the generated token. Not valid when includePrefix is NO.
     */
    public String getPrefixOverride() {
        return prefixOverride;
    }

    @Override
    public String toString() {
        return "Entity{" +
                "entityType=" + entityType +
                ", deidentificationType=" + deidentificationType +
                ", destination=" + destination +
                ", transformation=" + transformation +
                ", includePrefix=" + includePrefix +
                ", prefixOverride=" + prefixOverride +
                '}';
    }

    public static final class EntityBuilder {
        private EntityType entityType;
        private DeidentificationType deidentificationType;
        private String destination;
        private Transformation transformation;
        private IncludePrefix includePrefix;
        private String prefixOverride;

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

        /**
         * @param includePrefix Whether the entity label is included as a prefix in the generated token. Defaults to YES on the server. Only used with VAULT_TOKEN. NO returns the raw vault token, which cannot be re-identified later.
         * @return this builder
         */
        public EntityBuilder includePrefix(IncludePrefix includePrefix) {
            this.includePrefix = includePrefix;
            return this;
        }

        /**
         * @param prefixOverride Replacement for the default entity-type prefix in the generated token, for example NAAM instead of NAME, producing [NAAM_&lt;token&gt;]. Not valid when includePrefix is NO.
         * @return this builder
         */
        public EntityBuilder prefixOverride(String prefixOverride) {
            this.prefixOverride = prefixOverride;
            return this;
        }

        public Entity build() {
            return new Entity(this);
        }
    }
}
