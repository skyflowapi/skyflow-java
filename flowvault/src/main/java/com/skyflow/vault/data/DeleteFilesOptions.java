package com.skyflow.vault.data;

public final class DeleteFilesOptions {
    private final RequestInterceptor interceptor;

    private DeleteFilesOptions(Builder builder) {
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

        public DeleteFilesOptions build() {
            return new DeleteFilesOptions(this);
        }
    }
}
