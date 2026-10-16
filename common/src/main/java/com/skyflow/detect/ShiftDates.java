package com.skyflow.detect;

/**
 * Shifts detected dates by a random number of days within the given range.
 */
public final class ShiftDates {
    private final Integer minDays;
    private final Integer maxDays;

    private ShiftDates(ShiftDatesBuilder builder) {
        this.minDays = builder.minDays;
        this.maxDays = builder.maxDays;
    }

    /**
     * @return a new builder
     */
    public static ShiftDatesBuilder builder() {
        return new ShiftDatesBuilder();
    }

    /**
     * @return Minimum number of days to shift.
     */
    public Integer getMinDays() {
        return minDays;
    }

    /**
     * @return Maximum number of days to shift.
     */
    public Integer getMaxDays() {
        return maxDays;
    }

    @Override
    public String toString() {
        return "ShiftDates{" +
                "minDays=" + minDays +
                ", maxDays=" + maxDays +
                '}';
    }

    public static final class ShiftDatesBuilder {
        private Integer minDays;
        private Integer maxDays;

        private ShiftDatesBuilder() {
        }

        /**
         * @param minDays Minimum number of days to shift.
         * @return this builder
         */
        public ShiftDatesBuilder minDays(Integer minDays) {
            this.minDays = minDays;
            return this;
        }

        /**
         * @param maxDays Maximum number of days to shift.
         * @return this builder
         */
        public ShiftDatesBuilder maxDays(Integer maxDays) {
            this.maxDays = maxDays;
            return this;
        }

        public ShiftDates build() {
            return new ShiftDates(this);
        }
    }
}
