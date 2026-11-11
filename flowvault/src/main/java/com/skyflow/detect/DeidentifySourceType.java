package com.skyflow.detect;

/**
 * How the input file is supplied to {@code deidentifyFile} on a flowvault vault: base64 content or an S3 presigned URL. Flowvault vaults do not accept a skyflow id as the
 * source, so there is no {@code SKYFLOW_ID} here.
 * Also the {@code getOutputType()} of {@code DeidentifyFileResponse} and {@code GetRunResponse}.
 */
public enum DeidentifySourceType implements DeidentifyFileSource {
    BASE64,
    PRESIGNED_URL;
}
