package com.skyflow.vault.data;

public final class InsertOptions {
    private final RequestInterceptor interceptor;

    private InsertOptions(Builder builder) {
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

        public InsertOptions build() {
            return new InsertOptions(this);
        }
    }
}
