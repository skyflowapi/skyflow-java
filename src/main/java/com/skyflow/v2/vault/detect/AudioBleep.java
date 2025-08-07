package com.skyflow.v2.vault.detect;

public class AudioBleep {
    private final AudioBleepBuilder builder;

    private AudioBleep(AudioBleepBuilder builder) {
        this.builder = builder;
    }

    public static AudioBleepBuilder builder() {
        return new AudioBleepBuilder();
    }

    public Double getGain() {
        return this.builder.gain;
    }

    public Double getFrequency() {
        return this.builder.frequency;
    }

    public Double getStartPadding() {
        return this.builder.startPadding;
    }

    public Double getStopPadding() {
        return this.builder.stopPadding;
    }

    public static final class AudioBleepBuilder {
        private Double gain;
        private Double frequency;
        private Double startPadding;
        private Double stopPadding;

        private AudioBleepBuilder() {
            // Default constructor
        }

        public AudioBleepBuilder gain(Double gain) {
            this.gain = gain;
            return this;
        }

        public AudioBleepBuilder frequency(Double frequency) {
            this.frequency = frequency;
            return this;
        }

        public AudioBleepBuilder startPadding(Double startPadding) {
            this.startPadding = startPadding;
            return this;
        }

        public AudioBleepBuilder stopPadding(Double stopPadding) {
            this.stopPadding = stopPadding;
            return this;
        }

        public AudioBleep build() {
            return new AudioBleep(this);
        }
    }
}