package com.skyflow.vault.data;

/**
 * Per-call options for bulk tokenize.
 *
 * <p>Adds nothing to {@link TokenizeOptions} today; it exists so the bulk interfaces have their own
 * options type to grow into, matching the {@link BulkTokenizeRequest} / {@link TokenizeRequest} split.
 */
public final class BulkTokenizeOptions extends TokenizeOptions {

    private BulkTokenizeOptions(BulkTokenizeOptionsBuilder builder) {
        super(builder);
    }

    public static BulkTokenizeOptionsBuilder builder() {
        return new BulkTokenizeOptionsBuilder();
    }

    public static final class BulkTokenizeOptionsBuilder extends Builder {

        private BulkTokenizeOptionsBuilder() {}

        @Override
        public BulkTokenizeOptionsBuilder interceptor(RequestInterceptor interceptor) {
            super.interceptor(interceptor);
            return this;
        }

        @Override
        public BulkTokenizeOptions build() {
            return new BulkTokenizeOptions(this);
        }
    }
}
