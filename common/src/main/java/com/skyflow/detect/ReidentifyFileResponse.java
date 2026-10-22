package com.skyflow.detect;

import java.util.List;

/**
 * Response of {@code reidentifyFile}. File re-identification is synchronous, so a {@code SUCCESS}
 * status carries the output in the same response.
 */
public final class ReidentifyFileResponse {
    private final DetectRunStatus status;
    private final DataSourceType outputType;
    private final List<FileOutput> output;
    private final Metrics metrics;

    public ReidentifyFileResponse(DetectRunStatus status, DataSourceType outputType, List<FileOutput> output, Metrics metrics) {
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
     * @return How output files are delivered, or null when not reported.
     */
    public DataSourceType getOutputType() {
        return outputType;
    }

    /**
     * @return Output artifacts. Empty when none were returned, never null.
     */
    public List<FileOutput> getOutput() {
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
