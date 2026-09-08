package com.skyflow.vault.data;

public class QueryRequest extends BaseQueryRequest {

    protected QueryRequest(QueryRequestBuilder builder) {
        super(builder);
    }

    public static QueryRequestBuilder builder() {
        return new QueryRequestBuilder();
    }

    public static final class QueryRequestBuilder extends BaseQueryRequestBuilder {

        private QueryRequestBuilder() {
        }

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
