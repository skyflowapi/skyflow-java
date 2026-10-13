package com.skyflow.detect;

/**
 * PDF processing options.
 */
public final class Pdf {
    private final PdfProcessingMode processingMode;
    private final Integer density;
    private final Integer maxResolution;

    private Pdf(PdfBuilder builder) {
        this.processingMode = builder.processingMode;
        this.density = builder.density;
        this.maxResolution = builder.maxResolution;
    }

    /**
     * @return a new builder
     */
    public static PdfBuilder builder() {
        return new PdfBuilder();
    }

    /**
     * @return Processing mode.
     */
    public PdfProcessingMode getProcessingMode() {
        return processingMode;
    }

    /**
     * @return Rendering density (DPI).
     */
    public Integer getDensity() {
        return density;
    }

    /**
     * @return Maximum rendering resolution.
     */
    public Integer getMaxResolution() {
        return maxResolution;
    }

    @Override
    public String toString() {
        return "Pdf{" +
                "processingMode=" + processingMode +
                ", density=" + density +
                ", maxResolution=" + maxResolution +
                '}';
    }

    public static final class PdfBuilder {
        private PdfProcessingMode processingMode;
        private Integer density;
        private Integer maxResolution;

        private PdfBuilder() {
        }

        /**
         * @param processingMode Processing mode.
         * @return this builder
         */
        public PdfBuilder processingMode(PdfProcessingMode processingMode) {
            this.processingMode = processingMode;
            return this;
        }

        /**
         * @param density Rendering density (DPI).
         * @return this builder
         */
        public PdfBuilder density(Integer density) {
            this.density = density;
            return this;
        }

        /**
         * @param maxResolution Maximum rendering resolution.
         * @return this builder
         */
        public PdfBuilder maxResolution(Integer maxResolution) {
            this.maxResolution = maxResolution;
            return this;
        }

        public Pdf build() {
            return new Pdf(this);
        }
    }
}
