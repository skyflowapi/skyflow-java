package com.skyflow.logs;

public enum WarningLogs {
    INVALID_BATCH_SIZE_PROVIDED("Invalid value for batch size provided, switching to default value."),
    INVALID_CONCURRENCY_LIMIT_PROVIDED("Invalid value for concurrency limit provided, switching to default value."),
    BATCH_SIZE_EXCEEDS_MAX_LIMIT("Provided batch size exceeds the maximum limit, switching to max limit."),
    CONCURRENCY_EXCEEDS_MAX_LIMIT("Provided concurrency limit exceeds the maximum limit, switching to max limit."),
    EMPTY_DELETE_TOKENS_RESPONSE("DeleteTokens response did not include any token results."),
    INCOMPLETE_DELETE_TOKENS_RESPONSE("DeleteTokens response did not account for all requested tokens."),
    EMPTY_TOKENIZE_RESPONSE("Tokenize response did not include any record results."),
    INCOMPLETE_UPLOAD_FILES_RESPONSE("UploadFiles response did not return one record per requested record; no files were uploaded."),
    UPLOAD_FILES_RECORD_MISMATCH("UploadFiles response record at index %s1 does not match the requested table or skyflow ID; its files were not uploaded."),
    INCOMPLETE_TOKENIZE_RESPONSE("Tokenize response did not account for all requested records."),
    BEARER_TOKEN_REGENERATION_AFTER_401_FAILED("Bearer token regeneration after 401 failed; returning the 401 response."),
    BEARER_TOKEN_REGENERATION_RETURNED_REJECTED_TOKEN("Bearer token regeneration after 401 returned the rejected token; not resending.")
    ;

    private final String log;

    WarningLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
