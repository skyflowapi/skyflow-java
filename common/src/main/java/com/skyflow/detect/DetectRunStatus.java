package com.skyflow.detect;

/**
 * Lifecycle status of an asynchronous Detect file run.
 */
public enum DetectRunStatus {
    UNKNOWN,
    QUEUED,
    IN_PROGRESS,
    SUCCESS,
    FAILED;
}
