package com.skyflow.detect;

import java.util.List;

/**
 * Response of {@code deidentifyString}.
 */
public final class DeidentifyStringResponse {
    private final String processedText;
    private final List<DetectedEntity> entities;
    private final Metrics metrics;

    public DeidentifyStringResponse(String processedText, List<DetectedEntity> entities, Metrics metrics) {
        this.processedText = processedText;
        this.entities = entities;
        this.metrics = metrics;
    }

    /**
     * @return De-identified text.
     */
    public String getProcessedText() {
        return processedText;
    }

    /**
     * @return Detected entities. Never null.
     */
    public List<DetectedEntity> getEntities() {
        return entities;
    }

    /**
     * @return Processing metrics.
     */
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "DeidentifyStringResponse{" +
                "processedText=" + processedText +
                ", entities=" + entities +
                ", metrics=" + metrics +
                '}';
    }
}
