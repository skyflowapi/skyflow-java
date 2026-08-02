package com.skyflow.vault.data;

public class QueryRequest {
    private final QueryRequestBuilder builder;

    private QueryRequest(QueryRequestBuilder builder) {
        this.builder = builder;
    }

    public static QueryRequestBuilder builder() {
        return new QueryRequestBuilder();
    }

    public String getQuery() {
        return this.builder.query;
    }

    public static final class QueryRequestBuilder {
        private String query;

        private QueryRequestBuilder() {
        }

        public QueryRequestBuilder query(String query) {
            this.query = query;
            return this;
        }

        public QueryRequest build() {
            return new QueryRequest(this);
        }
    }
}
