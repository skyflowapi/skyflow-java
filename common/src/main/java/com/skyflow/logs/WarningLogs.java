package com.skyflow.logs;

public enum WarningLogs {
    INVALID_BATCH_SIZE_PROVIDED("Invalid value for batch size provided, switching to default value."),
    INVALID_CONCURRENCY_LIMIT_PROVIDED("Invalid value for concurrency limit provided, switching to default value."),
    BATCH_SIZE_EXCEEDS_MAX_LIMIT("Provided batch size exceeds the maximum limit, switching to max limit."),
    CONCURRENCY_EXCEEDS_MAX_LIMIT("Provided concurrency limit exceeds the maximum limit, switching to max limit.")
    ;

    private final String log;

    WarningLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
