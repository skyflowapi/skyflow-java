package com.skyflow.detect;

import java.util.List;

/**
 * Response of {@code deidentifyFile}. {@code runId} is always present. {@code status} is null when the
 * request did not poll; retrieve the run later with {@code getRun}. When polled, the remaining fields
 * mirror {@link GetRunResponse}: output, outputType and metrics are populated only on SUCCESS, and
 * message carries the failure detail on FAILED.
 */
public final class DeidentifyFileResponse {
    private final String runId;
    private final DetectRunStatus status;
    private final DataSourceType outputType;
    private final List<FileOutput> output;
    private final Metrics metrics;
    private final String message;

    public DeidentifyFileResponse(String runId, DetectRunStatus status, DataSourceType outputType,
                                  List<FileOutput> output, Metrics metrics, String message) {
        this.runId = runId;
        this.status = status;
        this.outputType = outputType;
        this.output = output;
        this.metrics = metrics;
        this.message = message;
    }

    /**
     * @return Run id returned by the submit call. Use it with {@code getRun} to retrieve or resume.
     */
    public String getRunId() {
        return runId;
    }

    /**
     * @return Run status after polling. Null when the request did not poll. IN_PROGRESS or QUEUED when the
     * polling budget ran out before the run finished.
     */
    public DetectRunStatus getStatus() {
        return status;
    }

    /**
     * @return How output files are delivered. Null unless polled to SUCCESS.
     */
    public DataSourceType getOutputType() {
        return outputType;
    }

    /**
     * @return Output artifacts. Empty unless polled to SUCCESS, never null.
     */
    public List<FileOutput> getOutput() {
        return output;
    }

    /**
     * @return Processing metrics. Null unless polled to SUCCESS.
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
        return "DeidentifyFileResponse{" +
                "runId=" + runId +
                ", status=" + status +
                ", outputType=" + outputType +
                ", output=" + output +
                ", metrics=" + metrics +
                ", message=" + message +
                '}';
    }
}
