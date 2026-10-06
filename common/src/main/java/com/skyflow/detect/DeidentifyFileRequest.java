package com.skyflow.detect;

import java.io.File;

/**
 * Request for {@code deidentifyFile}. Supply the file through exactly one of {@code value} (with a
 * {@code dataSource}), {@code file} or {@code filePath}. The two file forms are builder sugar that set
 * {@code dataSource = BASE64}, encode the content into {@code value} when the request is sent, and infer
 * {@code dataFormat} from the extension unless it is set explicitly.
 *
 * <p>Without {@code pollOptions} the call submits the run and returns its id. With {@code pollOptions} the
 * SDK polls the run and, on success for a file-based request, writes the outputs to disk.
 */
public final class DeidentifyFileRequest {
    private final DataSourceType dataSource;
    private final String value;
    private final File file;
    private final FileDataFormat dataFormat;
    private final String configurationId;
    private final DetectConfiguration configuration;
    private final PollOptions pollOptions;
    private final String outputDirectory;

    private DeidentifyFileRequest(DeidentifyFileRequestBuilder builder) {
        this.dataSource = builder.dataSource;
        this.value = builder.value;
        this.file = builder.file;
        this.dataFormat = builder.dataFormat;
        this.configurationId = builder.configurationId;
        this.configuration = builder.configuration;
        this.pollOptions = builder.pollOptions;
        this.outputDirectory = builder.outputDirectory;
    }

    /**
     * @return a new builder
     */
    public static DeidentifyFileRequestBuilder builder() {
        return new DeidentifyFileRequestBuilder();
    }

    /**
     * @return How {@code value} supplies the file. Required unless {@code file} or {@code filePath} was used.
     */
    public DataSourceType getDataSource() {
        return dataSource;
    }

    /**
     * @return Base64 content, skyflow id or presigned URL, according to {@code dataSource}.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return Local file to de-identify, when {@code file} or {@code filePath} was used.
     */
    public File getFile() {
        return file;
    }

    /**
     * @return Format of the file. Optional; inferred from the file extension or by the server when omitted.
     */
    public FileDataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * @return Id of a stored Detect configuration.
     */
    public String getConfigurationId() {
        return configurationId;
    }

    /**
     * @return Inline Detect configuration.
     */
    public DetectConfiguration getConfiguration() {
        return configuration;
    }

    /**
     * @return Polling settings, or null to submit only.
     */
    public PollOptions getPollOptions() {
        return pollOptions;
    }

    /**
     * @return Directory the outputs are written to on success. Only with {@code pollOptions} and a file-based request.
     */
    public String getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public String toString() {
        return "DeidentifyFileRequest{" +
                "dataSource=" + dataSource +
                ", value=" + (value == null ? null : "<" + value.length() + " chars>") +
                ", file=" + file +
                ", dataFormat=" + dataFormat +
                ", configurationId=" + configurationId +
                ", configuration=" + configuration +
                ", pollOptions=" + pollOptions +
                ", outputDirectory=" + outputDirectory +
                '}';
    }

    public static final class DeidentifyFileRequestBuilder {
        private DataSourceType dataSource;
        private String value;
        private File file;
        private FileDataFormat dataFormat;
        private String configurationId;
        private DetectConfiguration configuration;
        private PollOptions pollOptions;
        private String outputDirectory;

        private DeidentifyFileRequestBuilder() {
        }

        /**
         * @param dataSource How {@code value} supplies the file.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder dataSource(DataSourceType dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * @param value Base64 content, skyflow id or presigned URL, according to {@code dataSource}.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * @param file Local file to de-identify. Sets {@code dataSource} to BASE64; the content is encoded when sent.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder file(File file) {
            this.file = file;
            if (file != null) {
                this.dataSource = DataSourceType.BASE64;
            }
            return this;
        }

        /**
         * @param filePath Path of a local file to de-identify. Same as {@link #file(File)}.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder filePath(String filePath) {
            return file(filePath == null ? null : new File(filePath));
        }

        /**
         * @param dataFormat Format of the file. Optional.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder dataFormat(FileDataFormat dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        /**
         * @param configurationId Id of a stored Detect configuration.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder configurationId(String configurationId) {
            this.configurationId = configurationId;
            return this;
        }

        /**
         * @param configuration Inline Detect configuration.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder configuration(DetectConfiguration configuration) {
            this.configuration = configuration;
            return this;
        }

        /**
         * @param pollOptions Poll the run after submitting. Null to submit only.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder pollOptions(PollOptions pollOptions) {
            this.pollOptions = pollOptions;
            return this;
        }

        /**
         * @param outputDirectory Where to write outputs on success. Requires {@code pollOptions} and a file-based request.
         * @return this builder
         */
        public DeidentifyFileRequestBuilder outputDirectory(String outputDirectory) {
            this.outputDirectory = outputDirectory;
            return this;
        }

        public DeidentifyFileRequest build() {
            return new DeidentifyFileRequest(this);
        }
    }
}
