package com.skyflow.vault.detect;

import java.util.List;

/**
 * Response of {@code reidentifyFile}. File re-identification is synchronous, so a {@code SUCCESS}
 * status carries the output in the same response.
 */
public final class ReidentifyFileResponse {
    private final DetectRunStatus status;
    private final ReidentifyFileSource outputType;
    private final List<ReidentifyFileOutput> output;
    private final Metrics metrics;

    public ReidentifyFileResponse(DetectRunStatus status, ReidentifyFileSource outputType, List<ReidentifyFileOutput> output, Metrics metrics) {
        this.status = status;
        this.outputType = outputType;
        this.output = output;
        this.metrics = metrics;
    }

    /**
     * @return Processing status. UNKNOWN when the API did not report one.
     */
    public DetectRunStatus getStatus() {
        return status;
    }

    /**
     * @return How output files are delivered, or null when not reported. A constant of this SDK's {@code ReidentifySourceType}: {@code BASE64} or {@code PRESIGNED_URL}.
     */
    public ReidentifyFileSource getOutputType() {
        return outputType;
    }

    /**
     * @return Output artifacts. Empty when none were returned, never null.
     */
    public List<ReidentifyFileOutput> getOutput() {
        return output;
    }

    /**
     * @return Processing metrics, or null when not reported.
     */
    public Metrics getMetrics() {
        return metrics;
    }

    @Override
    public String toString() {
        return "ReidentifyFileResponse{" +
                "status=" + status +
                ", outputType=" + outputType +
                ", output=" + output +
                ", metrics=" + metrics +
                '}';
    }
}
