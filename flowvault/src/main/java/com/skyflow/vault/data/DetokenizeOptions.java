package com.skyflow.vault.data;

public class DetokenizeOptions {
    private final RequestInterceptor interceptor;

    protected DetokenizeOptions(Builder builder) {
        this.interceptor = builder.interceptor;
    }

    public RequestInterceptor getInterceptor() {
        return interceptor;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RequestInterceptor interceptor;

        protected Builder() {
        }

        public Builder interceptor(RequestInterceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public DetokenizeOptions build() {
            return new DetokenizeOptions(this);
        }
    }
}
