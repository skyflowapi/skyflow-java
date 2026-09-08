package com.skyflow.vault.data;

import java.util.List;
import java.util.Map;

// Deletes records by skyflowId or unique value. Distinct from DeleteTokensRequest, which
// removes tokens only and leaves the underlying record in place.
public class DeleteRequest {
    private final DeleteRequestBuilder builder;

    protected DeleteRequest(DeleteRequestBuilder builder) {
        this.builder = builder;
    }

    public String getTable() {
        return this.builder.table;
    }

    /** Either this or {@code uniqueValues} is required; specifying both fails validation. */
    public List<String> getIds() {
        return this.builder.ids;
    }

    public List<Map<String, Object>> getUniqueValues() {
        return this.builder.uniqueValues;
    }

    public static DeleteRequestBuilder builder() {
        return new DeleteRequestBuilder();
    }

    public static final class DeleteRequestBuilder {
        private String table;
        private List<String> ids;
        private List<Map<String, Object>> uniqueValues;

        protected DeleteRequestBuilder() {
        }

        public DeleteRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public DeleteRequestBuilder ids(List<String> ids) {
            this.ids = ids;
            return this;
        }

        public DeleteRequestBuilder uniqueValues(List<Map<String, Object>> uniqueValues) {
            this.uniqueValues = uniqueValues;
            return this;
        }

        public DeleteRequest build() {
            return new DeleteRequest(this);
        }
    }
}
