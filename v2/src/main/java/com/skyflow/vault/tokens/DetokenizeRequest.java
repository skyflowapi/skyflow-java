package com.skyflow.vault.tokens;

import com.skyflow.enums.RedactionType;
import com.skyflow.generated.rest.models.V1DetokenizePayload;
import com.skyflow.generated.rest.models.V1DetokenizeRecordRequest;

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

    public V1DetokenizePayload getDetokenizePayload() {
        V1DetokenizePayload payload = new V1DetokenizePayload();
        payload.setContinueOnError(this.getContinueOnError());
        for (String token : this.getTokens()) {
            V1DetokenizeRecordRequest recordRequest = new V1DetokenizeRecordRequest();
            recordRequest.setToken(token);
            recordRequest.setRedaction(this.getRedactionType().getRedaction());
            payload.addDetokenizationParametersItem(recordRequest);
        }
        return payload;
    }

    public static final class DetokenizeRequestBuilder {
        private ArrayList<String> tokens;
        private RedactionType redactionType;
        private Boolean continueOnError;

        public DetokenizeRequestBuilder() {
            this.tokens = new ArrayList<>();
            this.redactionType = RedactionType.PLAIN_TEXT;
            this.continueOnError = true;
        }

        public DetokenizeRequestBuilder tokens(ArrayList<String> tokens) {
            this.tokens = tokens;
            return this;
        }

        public DetokenizeRequestBuilder redactionType(RedactionType redactionType) {
            this.redactionType = redactionType;
            return this;
        }

        public DetokenizeRequestBuilder continueOnError(Boolean continueOnError) {
            this.continueOnError = continueOnError;
            return this;
        }

        public DetokenizeRequest build() {
            return new DetokenizeRequest(this);
        }
    }
}
