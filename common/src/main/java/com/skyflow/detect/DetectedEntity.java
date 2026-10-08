package com.skyflow.detect;

import java.util.Map;

/**
 * An entity detected in the input text.
 */
public final class DetectedEntity {
    private final String token;
    private final String value;
    private final EntityLocation location;
    private final String entityType;
    private final Map<String, Double> entityScores;

    public DetectedEntity(String token, String value, EntityLocation location, String entityType, Map<String, Double> entityScores) {
        this.token = token;
        this.value = value;
        this.location = location;
        this.entityType = entityType;
        this.entityScores = entityScores;
    }

    /**
     * @return Replacement token, for example NAME_1.
     */
    public String getToken() {
        return token;
    }

    /**
     * @return Original value. Empty unless returnEntities is ALL.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return Where the entity was found.
     */
    public EntityLocation getLocation() {
        return location;
    }

    /**
     * @return Entity type. Carries custom-type labels as well as built-in types.
     */
    public String getEntityType() {
        return entityType;
    }

    /**
     * @return Confidence scores by entity type, 0.0 to 1.0.
     */
    public Map<String, Double> getEntityScores() {
        return entityScores;
    }

    @Override
    public String toString() {
        return "DetectedEntity{" +
                "token=" + token +
                ", value=" + value +
                ", location=" + location +
                ", entityType=" + entityType +
                ", entityScores=" + entityScores +
                '}';
    }
}
