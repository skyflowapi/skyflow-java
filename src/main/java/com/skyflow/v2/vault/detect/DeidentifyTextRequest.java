package com.skyflow.v2.vault.detect;


import com.skyflow.v2.enums.DetectEntities;

import java.util.List;

public class DeidentifyTextRequest {
    private final DeidentifyTextRequestBuilder builder;

    private DeidentifyTextRequest(DeidentifyTextRequestBuilder builder) {
        this.builder = builder;
    }

    public static DeidentifyTextRequestBuilder builder() {
        return new DeidentifyTextRequestBuilder();
    }

    public String getText() {
        return this.builder.text;
    }

    public List<DetectEntities> getEntities() {
        return this.builder.entities;
    }

    public List<String> getAllowRegexList() {
        return this.builder.allowRegexList;
    }

    public List<String> getRestrictRegexList() {
        return this.builder.restrictRegexList;
    }

    public TokenFormat getTokenFormat() {
        return this.builder.tokenFormat;
    }

    public Transformations getTransformations() {
        return this.builder.transformations;
    }

    public static final class DeidentifyTextRequestBuilder {
        private String text;
        private List<DetectEntities> entities;
        private List<String> allowRegexList;
        private List<String> restrictRegexList;
        private TokenFormat tokenFormat;
        private Transformations transformations;

        private DeidentifyTextRequestBuilder() {
        }

        public DeidentifyTextRequestBuilder text(String text) {
            this.text = text;
            return this;
        }

        public DeidentifyTextRequestBuilder entities(List<DetectEntities> entities) {
            this.entities = entities;
            return this;
        }

        public DeidentifyTextRequestBuilder allowRegexList(List<String> allowRegexList) {
            this.allowRegexList = allowRegexList;
            return this;
        }

        public DeidentifyTextRequestBuilder restrictRegexList(List<String> restrictRegexList) {
            this.restrictRegexList = restrictRegexList;
            return this;
        }

        public DeidentifyTextRequestBuilder tokenFormat(TokenFormat tokenFormat) {
            this.tokenFormat = tokenFormat;
            return this;
        }

        public DeidentifyTextRequestBuilder transformations(Transformations transformations) {
            this.transformations = transformations;
            return this;
        }

        public DeidentifyTextRequest build() {
            return new DeidentifyTextRequest(this);
        }
    }
}