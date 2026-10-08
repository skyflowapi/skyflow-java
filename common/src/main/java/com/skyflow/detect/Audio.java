package com.skyflow.detect;

/**
 * Audio processing options.
 */
public final class Audio {
    private final Boolean outputProcessedAudio;
    private final OutputTranscriptionType outputTranscription;
    private final Bleep bleep;

    private Audio(AudioBuilder builder) {
        this.outputProcessedAudio = builder.outputProcessedAudio;
        this.outputTranscription = builder.outputTranscription;
        this.bleep = builder.bleep;
    }

    /**
     * @return a new builder
     */
    public static AudioBuilder builder() {
        return new AudioBuilder();
    }

    /**
     * @return Whether to return the processed audio file.
     */
    public Boolean getOutputProcessedAudio() {
        return outputProcessedAudio;
    }

    /**
     * @return Transcription output to produce.
     */
    public OutputTranscriptionType getOutputTranscription() {
        return outputTranscription;
    }

    /**
     * @return Bleep configuration.
     */
    public Bleep getBleep() {
        return bleep;
    }

    @Override
    public String toString() {
        return "Audio{" +
                "outputProcessedAudio=" + outputProcessedAudio +
                ", outputTranscription=" + outputTranscription +
                ", bleep=" + bleep +
                '}';
    }

    public static final class AudioBuilder {
        private Boolean outputProcessedAudio;
        private OutputTranscriptionType outputTranscription;
        private Bleep bleep;

        private AudioBuilder() {
        }

        /**
         * @param outputProcessedAudio Whether to return the processed audio file.
         * @return this builder
         */
        public AudioBuilder outputProcessedAudio(Boolean outputProcessedAudio) {
            this.outputProcessedAudio = outputProcessedAudio;
            return this;
        }

        /**
         * @param outputTranscription Transcription output to produce.
         * @return this builder
         */
        public AudioBuilder outputTranscription(OutputTranscriptionType outputTranscription) {
            this.outputTranscription = outputTranscription;
            return this;
        }

        /**
         * @param bleep Bleep configuration.
         * @return this builder
         */
        public AudioBuilder bleep(Bleep bleep) {
            this.bleep = bleep;
            return this;
        }

        public Audio build() {
            return new Audio(this);
        }
    }
}
