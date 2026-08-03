package com.skyflow.vault.data;

/**
 * Per-call options for bulk delete tokens.
 *
 * <p>Adds nothing to {@link DeleteTokensOptions} today; it exists so the bulk interfaces have their own
 * options type to grow into, matching the {@link BulkDeleteTokensRequest} / {@link DeleteTokensRequest} split.
 */
public final class BulkDeleteTokensOptions extends DeleteTokensOptions {

    private BulkDeleteTokensOptions(BulkDeleteTokensOptionsBuilder builder) {
        super(builder);
    }

    public static BulkDeleteTokensOptionsBuilder builder() {
        return new BulkDeleteTokensOptionsBuilder();
    }

    public static final class BulkDeleteTokensOptionsBuilder extends Builder {

        private BulkDeleteTokensOptionsBuilder() {}

        @Override
        public BulkDeleteTokensOptionsBuilder interceptor(RequestInterceptor interceptor) {
            super.interceptor(interceptor);
            return this;
        }

        @Override
        public BulkDeleteTokensOptions build() {
            return new BulkDeleteTokensOptions(this);
        }
    }
}
