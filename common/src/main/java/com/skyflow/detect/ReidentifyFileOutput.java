package com.skyflow.detect;

/**
 * One output artifact of {@code reidentifyFile}. The same shape as {@link FileOutput}, with the format reported as a
 * {@link ReidentifyFileDataFormat}, the formats re-identification works with.
 */
public final class ReidentifyFileOutput {
    private final String processedFile;
    private final String processedFileType;
    private final ReidentifyFileDataFormat processedFileExtension;

    public ReidentifyFileOutput(String processedFile, String processedFileType, ReidentifyFileDataFormat processedFileExtension) {
        this.processedFile = processedFile;
        this.processedFileType = processedFileType;
        this.processedFileExtension = processedFileExtension;
    }

    /**
     * @return Base64 content or presigned URL, according to the response's outputType.
     */
    public String getProcessedFile() {
        return processedFile;
    }

    /**
     * @return Kind of output, for example REIDENTIFIED_FILE. Reported as a string because the API does not enumerate it.
     */
    public String getProcessedFileType() {
        return processedFileType;
    }

    /**
     * @return Format of the output, or null when not reported or not a re-identification format.
     */
    public ReidentifyFileDataFormat getProcessedFileExtension() {
        return processedFileExtension;
    }

    @Override
    public String toString() {
        return "ReidentifyFileOutput{" +
                "processedFile=" + processedFile +
                ", processedFileType=" + processedFileType +
                ", processedFileExtension=" + processedFileExtension +
                '}';
    }
}
