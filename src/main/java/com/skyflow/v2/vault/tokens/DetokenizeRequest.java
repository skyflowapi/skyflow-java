package com.skyflow.v2.vault.tokens;

import java.util.ArrayList;

public class DetokenizeRequest {
    private final DetokenizeRequestBuilder builder;

    private DetokenizeRequest(DetokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder() {
        return new DetokenizeRequestBuilder();
    }

    public ArrayList<DetokenizeData> getDetokenizeData() {
        return this.builder.detokenizeData;
    }

    public Boolean getContinueOnError() {
        return this.builder.continueOnError;
    }

    public Boolean getDownloadURL() {
        return this.builder.downloadURL;
    }

    public static final class DetokenizeRequestBuilder {
        private ArrayList<DetokenizeData> detokenizeData;
        private Boolean continueOnError;
        private Boolean downloadURL;

        private DetokenizeRequestBuilder() {
            this.continueOnError = false;
            this.downloadURL = false;
        }

        public DetokenizeRequestBuilder detokenizeData(ArrayList<DetokenizeData> detokenizeData) {
            this.detokenizeData = detokenizeData;
            return this;
        }

        public DetokenizeRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError != null && continueOnError;
            return this;
        }

        public DetokenizeRequestBuilder downloadURL(Boolean downloadURL) {
            this.downloadURL = downloadURL;
            return this;
        }

        public DetokenizeRequest build() {
            return new DetokenizeRequest(this);
        }
    }
}
