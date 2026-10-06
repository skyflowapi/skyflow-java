package com.skyflow.detect;

/**
 * Opts {@code deidentifyFile} into submit-and-poll mode. When present, the SDK polls the run with
 * exponential backoff (1, 2, 4, 8, 16, then 32 seconds) until it reaches a terminal status, the
 * wait time is used up, or the attempt limit is hit. An empty {@code PollOptions.builder().build()}
 * uses the defaults.
 */
public final class PollOptions {
    public static final int DEFAULT_WAIT_TIME_SECONDS = 60;
    public static final int MAX_WAIT_TIME_SECONDS = 300;
    public static final int DEFAULT_MAX_ATTEMPTS = 23;
    public static final int MAX_MAX_ATTEMPTS = 23;

    private final Integer waitTime;
    private final Integer maxAttempts;

    private PollOptions(PollOptionsBuilder builder) {
        this.waitTime = builder.waitTime;
        this.maxAttempts = builder.maxAttempts;
    }

    /**
     * @return a new builder
     */
    public static PollOptionsBuilder builder() {
        return new PollOptionsBuilder();
    }

    /**
     * @return Maximum total seconds to keep polling, or null for the default of 60. At most 300.
     */
    public Integer getWaitTime() {
        return waitTime;
    }

    /**
     * @return Maximum number of run lookups, or null for the default of 23. At most 23.
     */
    public Integer getMaxAttempts() {
        return maxAttempts;
    }

    /** @return the wait time with the default applied */
    public int effectiveWaitTime() {
        return waitTime == null ? DEFAULT_WAIT_TIME_SECONDS : waitTime;
    }

    /** @return the attempt limit with the default applied */
    public int effectiveMaxAttempts() {
        return maxAttempts == null ? DEFAULT_MAX_ATTEMPTS : maxAttempts;
    }

    @Override
    public String toString() {
        return "PollOptions{" +
                "waitTime=" + waitTime +
                ", maxAttempts=" + maxAttempts +
                '}';
    }

    public static final class PollOptionsBuilder {
        private Integer waitTime;
        private Integer maxAttempts;

        private PollOptionsBuilder() {
        }

        /**
         * @param waitTime Maximum total seconds to keep polling. 1 to 300; default 60.
         * @return this builder
         */
        public PollOptionsBuilder waitTime(Integer waitTime) {
            this.waitTime = waitTime;
            return this;
        }

        /**
         * @param maxAttempts Maximum number of run lookups. 1 to 23; default 23.
         * @return this builder
         */
        public PollOptionsBuilder maxAttempts(Integer maxAttempts) {
            this.maxAttempts = maxAttempts;
            return this;
        }

        public PollOptions build() {
            return new PollOptions(this);
        }
    }
}
