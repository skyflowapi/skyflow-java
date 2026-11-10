package com.skyflow.detect;

/**
 * Transformations applied to detected entities before they are written to the output.
 */
public final class Transformation {
    private final ShiftDates shiftDates;

    private Transformation(TransformationBuilder builder) {
        this.shiftDates = builder.shiftDates;
    }

    /**
     * @return a new builder
     */
    public static TransformationBuilder builder() {
        return new TransformationBuilder();
    }

    /**
     * @return Date shifting configuration.
     */
    public ShiftDates getShiftDates() {
        return shiftDates;
    }

    @Override
    public String toString() {
        return "Transformation{" +
                "shiftDates=" + shiftDates +
                '}';
    }

    public static final class TransformationBuilder {
        private ShiftDates shiftDates;

        private TransformationBuilder() {
        }

        /**
         * @param shiftDates Date shifting configuration.
         * @return this builder
         */
        public TransformationBuilder shiftDates(ShiftDates shiftDates) {
            this.shiftDates = shiftDates;
            return this;
        }

        public Transformation build() {
            return new Transformation(this);
        }
    }
}
