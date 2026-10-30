package com.skyflow.detect;

/**
 * How the input file is supplied to {@code deidentifyFile}: base64 content, the skyflow id of a file
 * record in the vault, or an S3 presigned URL.
 */
public enum DeidentifySourceType {
    BASE64,
    SKYFLOW_ID,
    PRESIGNED_URL;
}
