package com.skyflow.vault.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DeleteFilesRequest extends BaseDeleteFilesRequest {
    private final List<DeleteFilesRequestRecord> records;

    private DeleteFilesRequest(DeleteFilesRequestBuilder builder) {
        super(builder);
        // Snapshot the list so later changes to the builder or the caller's list cannot alter a built request.
        this.records = builder.records == null ? null
                : Collections.unmodifiableList(new ArrayList<>(builder.records));
    }

    /** Records whose file columns to delete. */
    public List<DeleteFilesRequestRecord> getRecords() {
        return this.records;
    }

    public static DeleteFilesRequestBuilder builder() {
        return new DeleteFilesRequestBuilder();
    }

    public static final class DeleteFilesRequestBuilder extends BaseDeleteFilesRequestBuilder {
        private List<DeleteFilesRequestRecord> records;

        private DeleteFilesRequestBuilder() {
        }

        public DeleteFilesRequestBuilder records(List<DeleteFilesRequestRecord> records) {
            this.records = records;
            return this;
        }

        public DeleteFilesRequest build() {
            return new DeleteFilesRequest(this);
        }
    }
}
