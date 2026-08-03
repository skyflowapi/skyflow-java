package com.skyflow.vault.data;

// Bulk counterpart of InsertOptions. Carries no extra state today; the interceptor
// field is inherited.
public class BulkInsertOptions extends InsertOptions {

    protected BulkInsertOptions(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends InsertOptions.Builder {

        private Builder() {
        }

        @Override
        public Builder interceptor(RequestInterceptor interceptor) {
            super.interceptor(interceptor);
            return this;
        }

        @Override
        public BulkInsertOptions build() {
            return new BulkInsertOptions(this);
        }
    }
}
