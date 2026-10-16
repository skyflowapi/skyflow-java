package com.skyflow.detect;

/**
 * How file content is supplied to, or returned by, Detect file operations.
 */
public enum DataSourceType {
    BASE64,
    SKYFLOW_ID,
    PRESIGNED_URL;
}
