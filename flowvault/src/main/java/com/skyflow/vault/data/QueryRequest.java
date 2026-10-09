package com.skyflow.vault.data;

public class QueryRequest extends BaseQueryRequest {
    private QueryRequest(QueryRequestBuilder builder) {
        super(builder);
    }

    public static QueryRequestBuilder builder() {
        return new QueryRequestBuilder();
    }

    public static final class QueryRequestBuilder extends BaseQueryRequestBuilder {
        private QueryRequestBuilder() {
        }

        /** SQL {@code SELECT} statement to run. At most 25 records are returned; page with {@code OFFSET}. */
        @Override
        public QueryRequestBuilder query(String query) {
            super.query(query);
            return this;
        }

        public QueryRequest build() {
            return new QueryRequest(this);
        }
    }
}
