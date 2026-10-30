package com.skyflow.detect;

/**
 * Request for {@code getRun}: retrieves the status and outputs of a Detect file run.
 */
public final class GetRunRequest {
    private final String runId;

    private GetRunRequest(GetRunRequestBuilder builder) {
        this.runId = builder.runId;
    }

    /**
     * @return a new builder
     */
    public static GetRunRequestBuilder builder() {
        return new GetRunRequestBuilder();
    }

    /**
     * @return Run id returned by {@code deidentifyFile}. Required.
     */
    public String getRunId() {
        return runId;
    }

    @Override
    public String toString() {
        return "GetRunRequest{" +
                "runId=" + runId +
                '}';
    }

    public static final class GetRunRequestBuilder {
        private String runId;

        private GetRunRequestBuilder() {
        }

        /**
         * @param runId Run id returned by {@code deidentifyFile}. Required.
         * @return this builder
         */
        public GetRunRequestBuilder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public GetRunRequest build() {
            return new GetRunRequest(this);
        }
    }
}
