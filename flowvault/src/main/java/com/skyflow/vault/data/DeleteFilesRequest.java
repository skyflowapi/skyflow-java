package com.skyflow.vault.data;

import java.util.List;

public class DeleteFilesRequest extends BaseDeleteFilesRequest {
    private final DeleteFilesRequestBuilder builder;

    private DeleteFilesRequest(DeleteFilesRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    /** Records whose file columns to delete. */
    public List<DeleteFilesRequestRecord> getRecords() {
        return this.builder.records;
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
