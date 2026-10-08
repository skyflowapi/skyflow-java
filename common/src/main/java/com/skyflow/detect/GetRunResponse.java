package com.skyflow.detect;

import java.util.List;

/**
 * Response of {@code getRun}. Output, outputType and metrics are populated only once the run has succeeded.
 */
public final class GetRunResponse {
    private final String runId;
    private final DetectRunStatus status;
    private final DataSourceType outputType;
    private final List<FileOutput> output;
    private final Metrics metrics;
    private final String message;

    public GetRunResponse(String runId, DetectRunStatus status, DataSourceType outputType, List<FileOutput> output, Metrics metrics, String message) {
        this.runId = runId;
        this.status = status;
        this.outputType = outputType;
        this.output = output;
        this.metrics = metrics;
        this.message = message;
    }

    /**
     * @return Run id that was queried.
     */
    public String getRunId() {
        return runId;
    }

    /**
     * @return Run status. UNKNOWN when the API did not report one.
     */
    public DetectRunStatus getStatus() {
        return status;
    }

    /**
     * @return How output files are delivered. Null while the run is pending.
     */
    public DataSourceType getOutputType() {
        return outputType;
    }

    /**
     * @return Output artifacts. Empty while the run is pending, never null.
     */
    public List<FileOutput> getOutput() {
        return output;
    }

    /**
     * @return Processing metrics. Null while the run is pending.
     */
    public Metrics getMetrics() {
        return metrics;
    }

    /**
     * @return Failure detail when status is FAILED.
     */
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "GetRunResponse{" +
                "runId=" + runId +
                ", status=" + status +
                ", outputType=" + outputType +
                ", output=" + output +
                ", metrics=" + metrics +
                ", message=" + message +
                '}';
    }
}
