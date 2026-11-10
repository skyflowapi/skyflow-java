package com.skyflow.detect;

/**
 * Image processing options.
 */
public final class Image {
    private final Boolean outputProcessedImage;
    private final Boolean outputOcrText;
    private final MaskingMethod maskingMethod;

    private Image(ImageBuilder builder) {
        this.outputProcessedImage = builder.outputProcessedImage;
        this.outputOcrText = builder.outputOcrText;
        this.maskingMethod = builder.maskingMethod;
    }

    /**
     * @return a new builder
     */
    public static ImageBuilder builder() {
        return new ImageBuilder();
    }

    /**
     * @return Whether to return the processed image.
     */
    public Boolean getOutputProcessedImage() {
        return outputProcessedImage;
    }

    /**
     * @return Whether to return OCR text.
     */
    public Boolean getOutputOcrText() {
        return outputOcrText;
    }

    /**
     * @return Masking method for detected regions.
     */
    public MaskingMethod getMaskingMethod() {
        return maskingMethod;
    }

    @Override
    public String toString() {
        return "Image{" +
                "outputProcessedImage=" + outputProcessedImage +
                ", outputOcrText=" + outputOcrText +
                ", maskingMethod=" + maskingMethod +
                '}';
    }

    public static final class ImageBuilder {
        private Boolean outputProcessedImage;
        private Boolean outputOcrText;
        private MaskingMethod maskingMethod;

        private ImageBuilder() {
        }

        /**
         * @param outputProcessedImage Whether to return the processed image.
         * @return this builder
         */
        public ImageBuilder outputProcessedImage(Boolean outputProcessedImage) {
            this.outputProcessedImage = outputProcessedImage;
            return this;
        }

        /**
         * @param outputOcrText Whether to return OCR text.
         * @return this builder
         */
        public ImageBuilder outputOcrText(Boolean outputOcrText) {
            this.outputOcrText = outputOcrText;
            return this;
        }

        /**
         * @param maskingMethod Masking method for detected regions.
         * @return this builder
         */
        public ImageBuilder maskingMethod(MaskingMethod maskingMethod) {
            this.maskingMethod = maskingMethod;
            return this;
        }

        public Image build() {
            return new Image(this);
        }
    }
}
