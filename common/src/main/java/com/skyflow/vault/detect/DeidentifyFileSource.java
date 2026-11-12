package com.skyflow.vault.detect;

/**
 * How the file of {@code deidentifyFile} is supplied, and how a run reports its outputs. Each SDK ships its own
 * {@code com.skyflow.vault.detect.DeidentifySourceType} enum implementing this interface with the sources its vault
 * type accepts: skyvault {@code BASE64}, {@code SKYFLOW_ID}, {@code PRESIGNED_URL}; flowvault {@code BASE64},
 * {@code PRESIGNED_URL}. {@code getOutputType()} on {@code DeidentifyFileResponse} and {@code GetRunResponse}
 * returns a constant of that same enum. The constant name is the wire value.
 */
public interface DeidentifyFileSource {
    /**
     * @return the wire value, which is the constant name
     */
    String name();
}
