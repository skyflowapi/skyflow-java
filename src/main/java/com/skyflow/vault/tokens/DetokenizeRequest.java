package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;

import java.util.ArrayList;

public class DetokenizeRequest {
    private final DetokenizeRequestBuilder builder;

    private DetokenizeRequest(DetokenizeRequestBuilder builder) {
        this.builder = builder;
    }

    public static DetokenizeRequestBuilder builder() {
        return new DetokenizeRequestBuilder();
    }

    public ArrayList<String> getTokens() {
        return this.builder.tokens;
    }

    public RedactionType getRedactionType() {
        return this.builder.redactionType;
    }

    public Boolean getContinueOnError() {
        return this.builder.continueOnError;
    }

    public static final class DetokenizeRequestBuilder {
        private ArrayList<String> tokens;
        private RedactionType redactionType;
        private Boolean continueOnError;

        private DetokenizeRequestBuilder() {
            this.redactionType = RedactionType.PLAIN_TEXT;
            this.continueOnError = true;
        }

        public DetokenizeRequestBuilder tokens(ArrayList<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        public DetokenizeRequestBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType == null ? RedactionType.PLAIN_TEXT : redactionType;
            return this;
        }

        public DetokenizeRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError == null || continueOnError;
            return this;
        }

        public DetokenizeRequest build() {
            return new DetokenizeRequest(this);
        }
    }
}
