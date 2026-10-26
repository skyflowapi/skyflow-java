package com.skyflow.vault.data;

import java.util.List;

public class UploadFilesRequest extends BaseUploadFilesRequest {
    private final UploadFilesRequestBuilder builder;

    private UploadFilesRequest(UploadFilesRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    /** Records whose file columns to upload. One response record is returned per entry, in order. */
    public List<UploadFilesRequestRecord> getRecords() {
        return this.builder.records;
    }

    public static UploadFilesRequestBuilder builder() {
        return new UploadFilesRequestBuilder();
    }

    public static final class UploadFilesRequestBuilder extends BaseUploadFilesRequestBuilder {
        private List<UploadFilesRequestRecord> records;

        private UploadFilesRequestBuilder() {
        }

        public UploadFilesRequestBuilder records(List<UploadFilesRequestRecord> records) {
            this.records = records;
            return this;
        }

        public UploadFilesRequest build() {
            return new UploadFilesRequest(this);
        }
    }
}
