package com.skyflow.vault.data;

import com.skyflow.enums.CustomHeaderKey;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class RequestContext {
    /** Reported when the caller's request was not split into batches. */
    private static final int NOT_BATCHED = -1;

    private final String operation;
    private final int batchIndex;
    private final int totalBatches;
    private final Map<CustomHeaderKey, String> headers = new HashMap<>();

    public RequestContext(String operation) {
        this(operation, NOT_BATCHED, NOT_BATCHED);
    }

    public RequestContext(String operation, int batchIndex, int totalBatches) {
        this.operation = operation;
        this.batchIndex = batchIndex;
        this.totalBatches = totalBatches;
    }

    public String getOperation() { return operation; }

    /**
     * Zero-based position of this batch within the caller's request, or -1 when the operation was
     * not batched. Lets an interceptor tag each batch distinctly — a per-batch correlation id, for
     * instance — instead of seeing an identical context for every one.
     */
    public int getBatchIndex() { return batchIndex; }

    /** Total number of batches the request was split into, or -1 when it was not batched. */
    public int getTotalBatches() { return totalBatches; }

    public void addHeader(CustomHeaderKey key, String value) {
        headers.put(key, value);
    }

    public Map<CustomHeaderKey, String> getHeaders() {
        return Collections.unmodifiableMap(headers);
    }
}
