package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class UploadFilesRequest extends BaseUploadFilesRequest {
    private final List<UploadFilesRequestRecord> records;

    private UploadFilesRequest(UploadFilesRequestBuilder builder) {
        super(builder);
        // Snapshot the list so later changes to the builder or the caller's list cannot alter a built request.
        this.records = builder.records == null ? null
                : Collections.unmodifiableList(new ArrayList<>(builder.records));
    }

    /** Records whose file columns to upload. One response record is returned per entry, in order. */
    public List<UploadFilesRequestRecord> getRecords() {
        return this.records;
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
