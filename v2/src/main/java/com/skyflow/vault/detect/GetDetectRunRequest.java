package com.skyflow.vault.detect;

public class GetDetectRunRequest {
    private final String runId;

    private GetDetectRunRequest(GetDetectRunRequestBuilder builder) {
        this.runId = builder.runId;
    }

    public static GetDetectRunRequestBuilder builder() {
        return new GetDetectRunRequestBuilder();
    }

    public String getRunId() {
        return this.runId;
    }

    public static final class GetDetectRunRequestBuilder {
        private String runId;

        private GetDetectRunRequestBuilder() {
        }

        public GetDetectRunRequestBuilder runId(String runId) {
            this.runId = runId;
            return this;
        }

        public GetDetectRunRequest build() {
            return new GetDetectRunRequest(this);
        }
    }
}