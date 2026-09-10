package com.skyflow.vault.data;

public class DeleteOptions {
    private final RequestInterceptor interceptor;

    protected DeleteOptions(Builder builder) {
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

        public DeleteOptions build() {
            return new DeleteOptions(this);
        }
    }
}
