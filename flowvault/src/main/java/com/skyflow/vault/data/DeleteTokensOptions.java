package com.skyflow.vault.data;

/**
 * Per-call options for delete tokens.
 *
 * <p>Subclassed by {@link BulkDeleteTokensOptions} so the bulk interfaces can take their own options
 * type while sharing this one's settings, mirroring how {@link BulkDeleteTokensRequest} extends {@link DeleteTokensRequest}.
 */
public class DeleteTokensOptions {
    private final RequestInterceptor interceptor;

    protected DeleteTokensOptions(Builder builder) {
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

        protected Builder() {}

        public Builder interceptor(RequestInterceptor interceptor) {
            this.interceptor = interceptor;
            return this;
        }

        public DeleteTokensOptions build() {
            return new DeleteTokensOptions(this);
        }
    }
}
