package com.skyflow.vault.data;

public final class UploadFilesOptions {
    private final RequestInterceptor interceptor;

    private UploadFilesOptions(Builder builder) {
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

        public UploadFilesOptions build() {
            return new UploadFilesOptions(this);
        }
    }
}
