package com.skyflow.detect;

import java.util.List;

/**
 * Request for {@code reidentifyFile}. Supply the file through exactly one {@link DataSourceType}:
 * the base64-encoded content, the skyflow id of a vault record that holds the file, or an S3
 * presigned URL. The vault id is filled in by the controller.
 */
public final class ReidentifyFileRequest {
    private final DataSourceType dataSource;
    private final String value;
    private final FileDataFormat dataFormat;
    private final List<RedactionLevel> redactionLevel;

    private ReidentifyFileRequest(ReidentifyFileRequestBuilder builder) {
        this.dataSource = builder.dataSource;
        this.value = builder.value;
        this.dataFormat = builder.dataFormat;
        this.redactionLevel = builder.redactionLevel;
    }

    /**
     * @return a new builder
     */
    public static ReidentifyFileRequestBuilder builder() {
        return new ReidentifyFileRequestBuilder();
    }

    /**
     * @return How {@code value} supplies the file. Required.
     */
    public DataSourceType getDataSource() {
        return dataSource;
    }

    /**
     * @return Base64 content, skyflow id or presigned URL of the file, according to {@code dataSource}. Required.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return Format of the file. Optional; the API infers it when omitted. Re-identification supports text formats
     * such as TXT, CSV, JSON, JSONL, XML, DOCX, XLSX and PPTX.
     */
    public FileDataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * @return Optional per-group rendering rules.
     */
    public List<RedactionLevel> getRedactionLevel() {
        return redactionLevel;
    }

    @Override
    public String toString() {
        return "ReidentifyFileRequest{" +
                "dataSource=" + dataSource +
                ", value=" + value +
                ", dataFormat=" + dataFormat +
                ", redactionLevel=" + redactionLevel +
                '}';
    }

    public static final class ReidentifyFileRequestBuilder {
        private DataSourceType dataSource;
        private String value;
        private FileDataFormat dataFormat;
        private List<RedactionLevel> redactionLevel;

        private ReidentifyFileRequestBuilder() {
        }

        /**
         * @param dataSource How {@code value} supplies the file. Required.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder dataSource(DataSourceType dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * @param value Base64 content, skyflow id or presigned URL of the file, according to {@code dataSource}. Required.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * @param dataFormat Format of the file. Optional.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder dataFormat(FileDataFormat dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        /**
         * @param redactionLevel Optional per-group rendering rules.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder redactionLevel(List<RedactionLevel> redactionLevel) {
            this.redactionLevel = redactionLevel;
            return this;
        }

        public ReidentifyFileRequest build() {
            return new ReidentifyFileRequest(this);
        }
    }
}
