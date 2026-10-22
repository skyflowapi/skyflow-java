package com.skyflow.detect;

/**
 * Controls how one group of tokens is rendered when re-identifying. Supply exactly one source (entityName or tokenGroupName) and exactly one replacement (redactionType or redactionPattern).
 */
public final class RedactionLevel {
    private final EntityType entityName;
    private final RedactionType redactionType;
    private final String tokenGroupName;
    private final String redactionPattern;

    private RedactionLevel(RedactionLevelBuilder builder) {
        this.entityName = builder.entityName;
        this.redactionType = builder.redactionType;
        this.tokenGroupName = builder.tokenGroupName;
        this.redactionPattern = builder.redactionPattern;
    }

    /**
     * @return a new builder
     */
    public static RedactionLevelBuilder builder() {
        return new RedactionLevelBuilder();
    }

    /**
     * @return Entity to target. skyvault vaults.
     */
    public EntityType getEntityName() {
        return entityName;
    }

    /**
     * @return Replacement rendering. skyvault vaults.
     */
    public RedactionType getRedactionType() {
        return redactionType;
    }

    /**
     * @return Token group to target. flowvault vaults.
     */
    public String getTokenGroupName() {
        return tokenGroupName;
    }

    /**
     * @return Named redaction pattern to apply. flowvault vaults.
     */
    public String getRedactionPattern() {
        return redactionPattern;
    }

    @Override
    public String toString() {
        return "RedactionLevel{" +
                "entityName=" + entityName +
                ", redactionType=" + redactionType +
                ", tokenGroupName=" + tokenGroupName +
                ", redactionPattern=" + redactionPattern +
                '}';
    }

    public static final class RedactionLevelBuilder {
        private EntityType entityName;
        private RedactionType redactionType;
        private String tokenGroupName;
        private String redactionPattern;

        private RedactionLevelBuilder() {
        }

        /**
         * @param entityName Entity to target. skyvault vaults.
         * @return this builder
         */
        public RedactionLevelBuilder entityName(EntityType entityName) {
            this.entityName = entityName;
            return this;
        }

        /**
         * @param redactionType Replacement rendering. skyvault vaults.
         * @return this builder
         */
        public RedactionLevelBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        /**
         * @param tokenGroupName Token group to target. flowvault vaults.
         * @return this builder
         */
        public RedactionLevelBuilder tokenGroupName(String tokenGroupName) {
            this.tokenGroupName = tokenGroupName;
            return this;
        }

        /**
         * @param redactionPattern Named redaction pattern to apply. flowvault vaults.
         * @return this builder
         */
        public RedactionLevelBuilder redactionPattern(String redactionPattern) {
            this.redactionPattern = redactionPattern;
            return this;
        }

        public RedactionLevel build() {
            return new RedactionLevel(this);
        }
    }
}
