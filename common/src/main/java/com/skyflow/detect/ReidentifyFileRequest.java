package com.skyflow.detect;

import java.util.List;

/**
 * Request for {@code reidentifyFile}. Supply the file through this SDK's {@code ReidentifySourceType}
 * ({@code BASE64} or {@code PRESIGNED_URL}; re-identification does not accept a skyflow id) and the matching
 * {@code value}.
 */
public final class ReidentifyFileRequest {
    private final ReidentifyFileSource dataSource;
    private final String value;
    private final ReidentifyFileDataFormat dataFormat;
    private final List<? extends RedactionRule> redactionLevel;

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
     * @return How {@code value} supplies the file: this SDK's {@code ReidentifySourceType} (skyvault and flowvault:
     * BASE64, PRESIGNED_URL; a skyflow id is not offered for re-identification). Required.
     */
    public ReidentifyFileSource getDataSource() {
        return dataSource;
    }

    /**
     * @return Base64 content or presigned URL of the file, according to {@code dataSource}. Required.
     */
    public String getValue() {
        return value;
    }

    /**
     * @return Format of the file. Optional; the API infers it when omitted. Limited to the formats that support
     * {@code VAULT_TOKEN}: TXT, CSV, JSON, JSONL, XML, DOCX, XLSX and PPTX.
     */
    public ReidentifyFileDataFormat getDataFormat() {
        return dataFormat;
    }

    /**
     * @return Optional per-group rendering rules. Built with this SDK's {@code RedactionLevel}.
     */
    public List<? extends RedactionRule> getRedactionLevel() {
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
        private ReidentifyFileSource dataSource;
        private String value;
        private ReidentifyFileDataFormat dataFormat;
        private List<? extends RedactionRule> redactionLevel;

        private ReidentifyFileRequestBuilder() {
        }

        /**
         * @param dataSource How {@code value} supplies the file: this SDK's {@code ReidentifySourceType} (skyvault and
         *                   flowvault: BASE64, PRESIGNED_URL; a skyflow id is not offered for re-identification). Required.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder dataSource(ReidentifyFileSource dataSource) {
            this.dataSource = dataSource;
            return this;
        }

        /**
         * @param value Base64 content or presigned URL of the file, according to {@code dataSource}. Required.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder value(String value) {
            this.value = value;
            return this;
        }

        /**
         * @param dataFormat Format of the file. Optional. Only the formats that support {@code VAULT_TOKEN} are offered.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder dataFormat(ReidentifyFileDataFormat dataFormat) {
            this.dataFormat = dataFormat;
            return this;
        }

        /**
         * @param redactionLevel Optional rendering rules, built with this SDK's {@code RedactionLevel}.
         * @return this builder
         */
        public ReidentifyFileRequestBuilder redactionLevel(List<? extends RedactionRule> redactionLevel) {
            this.redactionLevel = redactionLevel;
            return this;
        }

        public ReidentifyFileRequest build() {
            return new ReidentifyFileRequest(this);
        }
    }
}
