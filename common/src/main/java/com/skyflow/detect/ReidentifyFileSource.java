package com.skyflow.detect;

/**
 * How the file of {@code reidentifyFile} is supplied, and how its response reports the output. Each SDK ships
 * its own {@code com.skyflow.detect.ReidentifySourceType} enum implementing this interface, with {@code BASE64}
 * and {@code PRESIGNED_URL} in both SDKs. {@code getOutputType()} on {@code ReidentifyFileResponse} returns a
 * constant of that same enum. The constant name is the wire value.
 */
public interface ReidentifyFileSource {
    /**
     * @return the wire value, which is the constant name
     */
    String name();
}
