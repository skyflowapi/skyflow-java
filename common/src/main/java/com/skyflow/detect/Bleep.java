package com.skyflow.detect;

/**
 * Bleep tone applied over detected speech in audio.
 */
public final class Bleep {
    private final Float startPadding;
    private final Float stopPadding;
    private final Integer frequency;
    private final Integer gain;

    private Bleep(BleepBuilder builder) {
        this.startPadding = builder.startPadding;
        this.stopPadding = builder.stopPadding;
        this.frequency = builder.frequency;
        this.gain = builder.gain;
    }

    /**
     * @return a new builder
     */
    public static BleepBuilder builder() {
        return new BleepBuilder();
    }

    /**
     * @return Seconds of bleep added before the detected span.
     */
    public Float getStartPadding() {
        return startPadding;
    }

    /**
     * @return Seconds of bleep added after the detected span.
     */
    public Float getStopPadding() {
        return stopPadding;
    }

    /**
     * @return Bleep frequency in Hz.
     */
    public Integer getFrequency() {
        return frequency;
    }

    /**
     * @return Bleep gain.
     */
    public Integer getGain() {
        return gain;
    }

    @Override
    public String toString() {
        return "Bleep{" +
                "startPadding=" + startPadding +
                ", stopPadding=" + stopPadding +
                ", frequency=" + frequency +
                ", gain=" + gain +
                '}';
    }

    public static final class BleepBuilder {
        private Float startPadding;
        private Float stopPadding;
        private Integer frequency;
        private Integer gain;

        private BleepBuilder() {
        }

        /**
         * @param startPadding Seconds of bleep added before the detected span.
         * @return this builder
         */
        public BleepBuilder startPadding(Float startPadding) {
            this.startPadding = startPadding;
            return this;
        }

        /**
         * @param stopPadding Seconds of bleep added after the detected span.
         * @return this builder
         */
        public BleepBuilder stopPadding(Float stopPadding) {
            this.stopPadding = stopPadding;
            return this;
        }

        /**
         * @param frequency Bleep frequency in Hz.
         * @return this builder
         */
        public BleepBuilder frequency(Integer frequency) {
            this.frequency = frequency;
            return this;
        }

        /**
         * @param gain Bleep gain.
         * @return this builder
         */
        public BleepBuilder gain(Integer gain) {
            this.gain = gain;
            return this;
        }

        public Bleep build() {
            return new Bleep(this);
        }
    }
}
