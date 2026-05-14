package com.skyflow.vault.data;

public final class DeleteTokensOptions {
    private final RequestInterceptor interceptor;

    private DeleteTokensOptions(Builder builder) {
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

        public DeleteTokensOptions build() {
            return new DeleteTokensOptions(this);
        }
    }
}
