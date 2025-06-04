package com.skyflow.vault.detect;

import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.DetectOutputTranscriptions;
import com.skyflow.enums.MaskingMethod;
import java.io.File;
import java.util.List;

public class DeidentifyFileRequest {
    private final DeidentifyFileRequestBuilder builder;

    private DeidentifyFileRequest(DeidentifyFileRequestBuilder builder) {
        this.builder = builder;
    }

    public static DeidentifyFileRequestBuilder builder() {
        return new DeidentifyFileRequestBuilder();
    }

    public File getFile() {
        return this.builder.file;
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

    public Boolean getOutputProcessedImage() {
        return this.builder.outputProcessedImage;
    }

    public Boolean getOutputOcrText() {
        return this.builder.outputOcrText;
    }

    public MaskingMethod getMaskingMethod() {
        return this.builder.maskingMethod;
    }

    public Double getPixelDensity() {
        return this.builder.pixelDensity;
    }

    public Double getMaxResolution() {
        return this.builder.maxResolution;
    }

    public Boolean getOutputProcessedAudio() {
        return this.builder.outputProcessedAudio;
    }

    public DetectOutputTranscriptions getOutputTranscription() {
        return this.builder.outputTranscription;
    }

    public AudioBleep getBleep() {
        return this.builder.bleep;
    }

    public String getOutputDirectory() {
        return this.builder.outputDirectory;
    }

    public Integer getWaitTime() {
        return this.builder.waitTime;
    }

    public static final class DeidentifyFileRequestBuilder {
        private File file;
        private List<DetectEntities> entities;
        private List<String> allowRegexList;
        private List<String> restrictRegexList;
        private TokenFormat tokenFormat;
        private Transformations transformations;
        private Boolean outputProcessedImage;
        private Boolean outputOcrText;
        private MaskingMethod maskingMethod;
        private Double pixelDensity;
        private Double maxResolution;
        private Boolean outputProcessedAudio;
        private DetectOutputTranscriptions outputTranscription;
        private AudioBleep bleep;
        private String outputDirectory;
        private Integer waitTime;

        private DeidentifyFileRequestBuilder() {
            // Set default values
            this.outputProcessedImage = false;
            this.outputOcrText = false;
            this.outputProcessedAudio = false;
        }

        public DeidentifyFileRequestBuilder file(File file) {
            this.file = file;
            return this;
        }

        public DeidentifyFileRequestBuilder entities(List<DetectEntities> entities) {
            this.entities = entities;
            return this;
        }

        public DeidentifyFileRequestBuilder allowRegexList(List<String> allowRegexList) {
            this.allowRegexList = allowRegexList;
            return this;
        }

        public DeidentifyFileRequestBuilder restrictRegexList(List<String> restrictRegexList) {
            this.restrictRegexList = restrictRegexList;
            return this;
        }

        public DeidentifyFileRequestBuilder tokenFormat(TokenFormat tokenFormat) {
            this.tokenFormat = tokenFormat;
            return this;
        }

        public DeidentifyFileRequestBuilder transformations(Transformations transformations) {
            this.transformations = transformations;
            return this;
        }

        public DeidentifyFileRequestBuilder outputProcessedImage(Boolean outputProcessedImage) {
            this.outputProcessedImage = outputProcessedImage != null ? outputProcessedImage : false;
            return this;
        }

        public DeidentifyFileRequestBuilder outputOcrText(Boolean outputOcrText) {
            this.outputOcrText = outputOcrText != null ? outputOcrText : false;
            return this;
        }

        public DeidentifyFileRequestBuilder maskingMethod(MaskingMethod maskingMethod) {
            this.maskingMethod = maskingMethod;
            return this;
        }

        public DeidentifyFileRequestBuilder pixelDensity(Double pixelDensity) {
            this.pixelDensity = pixelDensity;
            return this;
        }

        public DeidentifyFileRequestBuilder maxResolution(Double maxResolution) {
            this.maxResolution = maxResolution;
            return this;
        }

        public DeidentifyFileRequestBuilder outputProcessedAudio(Boolean outputProcessedAudio) {
            this.outputProcessedAudio = outputProcessedAudio != null ? outputProcessedAudio : false;
            return this;
        }

        public DeidentifyFileRequestBuilder outputTranscription(DetectOutputTranscriptions outputTranscription) {
            this.outputTranscription = outputTranscription;
            return this;
        }

        public DeidentifyFileRequestBuilder bleep(AudioBleep bleep) {
            this.bleep = bleep;
            return this;
        }

        public DeidentifyFileRequestBuilder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public DeidentifyFileRequestBuilder waitTime(Integer waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        public DeidentifyFileRequest build() {
            return new DeidentifyFileRequest(this);
        }
    }
}