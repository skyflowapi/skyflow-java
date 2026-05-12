package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class DetokenizeOptions {
    private final Map<CustomHeaderKey, String> customHeaders;

    private DetokenizeOptions(Builder builder) {
        this.customHeaders = Collections.unmodifiableMap(new HashMap<>(builder.customHeaders));
    }

    public Map<CustomHeaderKey, String> getCustomHeaders() {
        return customHeaders;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {
        private final Map<CustomHeaderKey, String> customHeaders = new HashMap<>();

        public Builder addCustomHeader(CustomHeaderKey key, String value) {
            this.customHeaders.put(key, value);
            return this;
        }

        public DetokenizeOptions build() {
            return new DetokenizeOptions(this);
        }
    }
}