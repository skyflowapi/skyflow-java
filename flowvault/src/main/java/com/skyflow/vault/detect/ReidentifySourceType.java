package com.skyflow.vault.detect;

/**
 * How the input file is supplied to {@code reidentifyFile} on a flowvault vault: base64 content or an S3 presigned
 * URL. Re-identification does not accept a skyflow id as the source. Also the {@code getOutputType()} of
 * {@code ReidentifyFileResponse}.
 */
public enum ReidentifySourceType implements ReidentifyFileSource {
    BASE64,
    PRESIGNED_URL;
}
