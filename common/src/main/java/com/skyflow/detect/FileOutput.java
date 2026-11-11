package com.skyflow.detect;

/**
 * One output artifact of a Detect file de-identification run, returned by {@code deidentifyFile} and {@code getRun}.
 * The format is reported as a {@link DeidentifyFileDataFormat}; {@code reidentifyFile} returns {@link ReidentifyFileOutput}.
 */
public final class FileOutput {
    private final String processedFile;
    private final String processedFileType;
    private final DeidentifyFileDataFormat processedFileExtension;

    public FileOutput(String processedFile, String processedFileType, DeidentifyFileDataFormat processedFileExtension) {
        this.processedFile = processedFile;
        this.processedFileType = processedFileType;
        this.processedFileExtension = processedFileExtension;
    }

    /**
     * @return Base64 content or presigned URL, according to the run's outputType.
     */
    public String getProcessedFile() {
        return processedFile;
    }

    /**
     * @return Kind of output, for example REDACTED_FILE or ENTITIES. Reported as a string because the API does not enumerate it.
     */
    public String getProcessedFileType() {
        return processedFileType;
    }

    /**
     * @return Format of the output, or null when not reported.
     */
    public DeidentifyFileDataFormat getProcessedFileExtension() {
        return processedFileExtension;
    }

    @Override
    public String toString() {
        return "FileOutput{" +
                "processedFile=" + processedFile +
                ", processedFileType=" + processedFileType +
                ", processedFileExtension=" + processedFileExtension +
                '}';
    }
}
