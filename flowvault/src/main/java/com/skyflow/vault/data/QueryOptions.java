package com.skyflow.vault.data;

public final class QueryOptions {
    private final RequestInterceptor interceptor;

    private QueryOptions(Builder builder) {
        this.interceptor = builder.interceptor;
    }

    public RequestInterceptor getInterceptor() {
        return interceptor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private RequestInterceptor interceptor;

        public Builder interceptor(RequestInterceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public QueryOptions build() {
            return new QueryOptions(this);
        }
    }
}
