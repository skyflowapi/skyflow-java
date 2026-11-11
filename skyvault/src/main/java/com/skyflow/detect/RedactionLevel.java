package com.skyflow.detect;

import com.skyflow.enums.RedactionType;

/**
 * Controls how one group of tokens is rendered when re-identifying on a skyvault vault: the {@code entityName}
 * the rule applies to and the {@code redactionType} to render it with ({@code PLAIN_TEXT}, {@code MASKED},
 * {@code REDACTED} or {@code DEFAULT}). Element of {@code redactionLevel} on {@code ReidentifyStringRequest} and
 * {@code ReidentifyFileRequest}. Both fields are optional on the API and are sent as given.
 */
public final class RedactionLevel extends RedactionRule {
    private final EntityType entityName;
    private final RedactionType redactionType;

    private RedactionLevel(RedactionLevelBuilder builder) {
        this.entityName = builder.entityName;
        this.redactionType = builder.redactionType;
    }

    /**
     * @return a new builder
     */
    public static RedactionLevelBuilder builder() {
        return new RedactionLevelBuilder();
    }

    /**
     * @return Entity type the rule applies to.
     */
    public EntityType getEntityName() {
        return entityName;
    }

    /**
     * @return Rendering applied to that entity's values.
     */
    public RedactionType getRedactionType() {
        return redactionType;
    }

    @Override
    String apiEntityName() {
        return entityName == null ? null : entityName.name();
    }

    @Override
    String apiRedactionType() {
        return redactionType == null ? null : redactionType.name();
    }

    @Override
    String apiTokenGroupName() {
        return null;
    }

    @Override
    String apiRedactionPattern() {
        return null;
    }

    @Override
    public String toString() {
        return "RedactionLevel{" +
                "entityName=" + entityName +
                ", redactionType=" + redactionType +
                '}';
    }

    public static final class RedactionLevelBuilder {
        private EntityType entityName;
        private RedactionType redactionType;

        private RedactionLevelBuilder() {
        }

        /**
         * @param entityName Entity type the rule applies to.
         * @return this builder
         */
        public RedactionLevelBuilder entityName(EntityType entityName) {
            this.entityName = entityName;
            return this;
        }

        /**
         * @param redactionType Rendering applied to that entity's values, the same {@code com.skyflow.enums.RedactionType}
         *                      used by {@code get} and {@code detokenize}.
         * @return this builder
         */
        public RedactionLevelBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        public RedactionLevel build() {
            return new RedactionLevel(this);
        }
    }
}
