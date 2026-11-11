package com.skyflow.detect;

/**
 * Response of {@code reidentifyString}.
 */
public final class ReidentifyStringResponse {
    private final String processedText;
    private final Metrics metrics;

    public ReidentifyStringResponse(String processedText, Metrics metrics) {
        this.processedText = processedText;
        this.metrics = metrics;
    }

    /**
     * @return Re-identified text.
     */
    public String getProcessedText() {
        return processedText;
    }

    /**
     * @return Processing metrics.
     */
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "ReidentifyStringResponse{" +
                "processedText=" + processedText +
                ", metrics=" + metrics +
                '}';
    }
}
