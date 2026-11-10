package com.skyflow.utils;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A successful (2xx) vault response whose body is not the JSON object the SDK expects. Raised
 * inside the HTTP chain by {@link SuccessBodyInterceptor} so the real status, body and headers
 * reach the controller, which turns it into a SkyflowException; without it the generated client
 * reports only a deserialization failure with no status.
 */
public final class UnparseableResponseException extends IOException {
    private final int statusCode;
    private final String body;
    private final transient Map<String, List<String>> headers;

    public UnparseableResponseException(int statusCode, String body, Map<String, List<String>> headers) {
        super("Unexpected non-JSON response body with status code " + statusCode);
        this.statusCode = statusCode;
        this.body = body;
        this.headers = headers;
    }

    public int getStatusCode() {
        return statusCode;
    }

    /** The response body as received, truncated for very large bodies. */
    public String getBody() {
        return body;
    }

    public Map<String, List<String>> getHeaders() {
        return headers;
    }
}
