package com.skyflow.vault.data;

public final class GetTokensOptions {
    private final RequestInterceptor interceptor;

    private GetTokensOptions(Builder builder) {
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

        private Builder() {
        }

        public Builder interceptor(RequestInterceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public GetTokensOptions build() {
            return new GetTokensOptions(this);
        }
    }
}
