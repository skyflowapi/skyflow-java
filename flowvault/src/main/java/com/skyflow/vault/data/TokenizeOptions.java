package com.skyflow.vault.data;

/**
 * Per-call options for tokenize.
 *
 * <p>Subclassed by {@link BulkTokenizeOptions} so the bulk interfaces can take their own options
 * type while sharing this one's settings, mirroring how {@link BulkTokenizeRequest} extends {@link TokenizeRequest}.
 */
public class TokenizeOptions {
    private final RequestInterceptor interceptor;

    protected TokenizeOptions(Builder builder) {
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

        public TokenizeOptions build() {
            return new TokenizeOptions(this);
        }
    }
}
