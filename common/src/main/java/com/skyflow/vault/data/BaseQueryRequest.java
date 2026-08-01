package com.skyflow.vault.data;

public class BaseQueryRequest {
    private final BaseQueryRequestBuilder builder;

    protected BaseQueryRequest(BaseQueryRequestBuilder builder) {
        this.builder = builder;
    }

    public String getQuery() {
        return this.builder.query;
    }

    static class BaseQueryRequestBuilder {
        protected String query;

        protected BaseQueryRequestBuilder() {
        }

        public BaseQueryRequestBuilder query(String query) {
            this.query = query;
            return this;
        }
    }
}
