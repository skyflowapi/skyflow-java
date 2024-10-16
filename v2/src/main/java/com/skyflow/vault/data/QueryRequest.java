package com.skyflow.vault.data;

public class QueryRequest {
    private final String query;

    private QueryRequest(QueryRequestBuilder builder) {
        this.query = builder.query;
    }

    public static QueryRequestBuilder builder() {
        return new QueryRequestBuilder();
    }

    public String getQuery() {
        return query;
    }

    public static class QueryRequestBuilder {
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
