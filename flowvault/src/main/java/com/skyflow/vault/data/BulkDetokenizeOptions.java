package com.skyflow.vault.data;

// Bulk counterpart of DetokenizeOptions. Carries no extra state today; the interceptor
// field is inherited.
public class BulkDetokenizeOptions extends DetokenizeOptions {

    protected BulkDetokenizeOptions(Builder builder) {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder extends DetokenizeOptions.Builder {

        private Builder() {
        }

        @Override
        public Builder interceptor(RequestInterceptor interceptor) {
            super.interceptor(interceptor);
            return this;
        }

        @Override
        public BulkDetokenizeOptions build() {
            return new BulkDetokenizeOptions(this);
        }
    }
}
