package com.skyflow.vault.detect;


import com.skyflow.enums.DetectEntities;

import java.util.List;

public class ReidentifyTextRequest {
    private final ReidentifyTextRequestBuilder builder;

    private ReidentifyTextRequest(ReidentifyTextRequestBuilder builder) {
        this.builder = builder;
    }

    public static ReidentifyTextRequestBuilder builder() {
        return new ReidentifyTextRequestBuilder();
    }

    public String getText() {
        return this.builder.text;
    }

    public List<DetectEntities> getRedactedEntities() {
        return this.builder.redactedEntities;
    }

    public List<DetectEntities> getMaskedEntities() {
        return this.builder.maskedEntities;
    }

    public List<DetectEntities> getPlainTextEntities() {
        return this.builder.plainTextEntities;
    }

    public static final class ReidentifyTextRequestBuilder {
        private String text;
        private List<DetectEntities> redactedEntities;
        private List<DetectEntities> maskedEntities;
        private List<DetectEntities> plainTextEntities;

        private ReidentifyTextRequestBuilder() {
        }

        public ReidentifyTextRequestBuilder text(String text) {
            this.text = text;
            return this;
        }

        public ReidentifyTextRequestBuilder redactedEntities(List<DetectEntities> redactedEntities) {
            this.redactedEntities = redactedEntities;
            return this;
        }

        public ReidentifyTextRequestBuilder maskedEntities(List<DetectEntities> maskedEntities) {
            this.maskedEntities = maskedEntities;
            return this;
        }

        public ReidentifyTextRequestBuilder plainTextEntities(List<DetectEntities> plainTextEntities) {
            this.plainTextEntities = plainTextEntities;
            return this;
        }

        public ReidentifyTextRequest build() {
            return new ReidentifyTextRequest(this);
        }
    }
}
