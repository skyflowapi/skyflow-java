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

    public String getTableName() {
        return this.builder.tableName;
    }

    /** Either this or {@code uniqueValues} is required; specifying both fails validation. */
    public List<String> getSkyflowIds() {
        return this.builder.skyflowIds;
    }

    public List<Map<String, Object>> getUniqueValues() {
        return this.builder.uniqueValues;
    }

    public static DeleteRequestBuilder builder() {
        return new DeleteRequestBuilder();
    }

    public static final class DeleteRequestBuilder {
        private String tableName;
        private List<String> skyflowIds;
        private List<Map<String, Object>> uniqueValues;

        protected DeleteRequestBuilder() {
        }

        public DeleteRequestBuilder tableName(String tableName) {
            this.tableName = tableName;
            return this;
        }

        public DeleteRequestBuilder skyflowIds(List<String> skyflowIds) {
            this.skyflowIds = skyflowIds;
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
