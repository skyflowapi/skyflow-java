package com.skyflow.detect;

/**
 * How the input file is supplied to {@code deidentifyFile} on a skyvault vault: base64 content, the skyflow id of a file record already in the vault, or an S3 presigned URL.
 * Also the {@code getOutputType()} of {@code DeidentifyFileResponse} and {@code GetRunResponse}.
 */
public enum DeidentifySourceType implements DeidentifyFileSource {
    BASE64,
    SKYFLOW_ID,
    PRESIGNED_URL;
}
