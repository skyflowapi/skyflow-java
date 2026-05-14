package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RequestContext {
    private final String operation;
    private final int batchIndex;
    private final int totalBatches;
    private final Map<CustomHeaderKey, String> headers = new HashMap<>();

    public RequestContext(String operation, int batchIndex, int totalBatches) {
        this.operation = operation;
        this.batchIndex = batchIndex;
        this.totalBatches = totalBatches;
    }

    public String getOperation() { return operation; }
    public int getBatchIndex() { return batchIndex; }
    public int getTotalBatches() { return totalBatches; }

    public void addHeader(CustomHeaderKey key, String value) {
        headers.put(key, value);
    }

    public Map<CustomHeaderKey, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
