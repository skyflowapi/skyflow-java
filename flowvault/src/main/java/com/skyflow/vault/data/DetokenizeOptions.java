package com.skyflow.vault.data;

public final class DetokenizeOptions {
    private final RequestInterceptor interceptor;

    private DetokenizeOptions(Builder builder) {
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

        public DetokenizeOptions build() {
            return new DetokenizeOptions(this);
        }
    }
}
