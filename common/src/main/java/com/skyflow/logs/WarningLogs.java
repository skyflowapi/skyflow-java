package com.skyflow.logs;

public enum WarningLogs {
    INVALID_BATCH_SIZE_PROVIDED("Invalid value for batch size provided, switching to default value."),
    INVALID_CONCURRENCY_LIMIT_PROVIDED("Invalid value for concurrency limit provided, switching to default value."),
    BATCH_SIZE_EXCEEDS_MAX_LIMIT("Provided batch size exceeds the maximum limit, switching to max limit."),
    CONCURRENCY_EXCEEDS_MAX_LIMIT("Provided concurrency limit exceeds the maximum limit, switching to max limit."),
    EMPTY_DELETE_TOKENS_RESPONSE("DeleteTokens response did not include any token results."),
    INCOMPLETE_DELETE_TOKENS_RESPONSE("DeleteTokens response did not account for all requested tokens."),
    EMPTY_TOKENIZE_RESPONSE("Tokenize response did not include any record results."),
    INCOMPLETE_TOKENIZE_RESPONSE("Tokenize response did not account for all requested records.")
    ;

    private final String log;

    WarningLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
