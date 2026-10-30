package com.skyflow.detect;

/**
 * How the input file is supplied to {@code reidentifyFile}: base64 content or an S3 presigned URL.
 * Re-identification does not accept a skyflow id as the source, so there is no {@code SKYFLOW_ID} here.
 */
public enum ReidentifySourceType {
    BASE64,
    PRESIGNED_URL;
}
