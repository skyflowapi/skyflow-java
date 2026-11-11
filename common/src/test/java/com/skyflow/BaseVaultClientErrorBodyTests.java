package com.skyflow;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class BaseVaultClientErrorBodyTests {

    /** Shape of the Fern-generated ErrorResponse: wire names via @JsonProperty, Optional fields. */
    public static final class TypedError {
        @JsonProperty("grpc_code") public Optional<Integer> grpcCode = Optional.of(5);
        @JsonProperty("http_code") public Optional<Integer> httpCode = Optional.of(404);
        @JsonProperty("http_status") public Optional<String> httpStatus = Optional.of("Not Found");
        @JsonProperty("message") public Optional<String> message = Optional.of("Detokenization failed. Token x is invalid.");
        @JsonProperty("details") public Optional<List<Object>> details = Optional.of(Collections.emptyList());
    }

    public static final class TypedErrorResponse {
        @JsonProperty("error") public TypedError error = new TypedError();
    }

    private static Map<String, List<String>> headers() {
        Map<String, List<String>> h = new HashMap<>();
        h.put("x-request-id", Collections.singletonList("req-1"));
        return h;
    }

    @Test
    public void typedErrorBodyIsParsedIntoTheExceptionFields() {
        SkyflowException e = BaseVaultClient.wrapApiException(404, new RuntimeException("boom"), headers(),
                new TypedErrorResponse(), ErrorLogs.DETOKENIZE_REQUEST_REJECTED);
        Assert.assertEquals(404, e.getHttpCode());
        Assert.assertEquals("Not Found", e.getHttpStatus());
        Assert.assertEquals(Integer.valueOf(5), e.getGrpcCode());
        Assert.assertEquals("Detokenization failed. Token x is invalid.", e.getMessage());
        Assert.assertEquals("req-1", e.getRequestId());
        Assert.assertEquals(0, e.getDetails().size());
    }

    @Test
    public void mapErrorBodyStillWorks() {
        Map<String, Object> error = new HashMap<>();
        error.put("grpc_code", 3);
        error.put("http_status", "Bad Request");
        error.put("message", "Insert failed. Column cv is invalid.");
        error.put("details", Collections.emptyList());
        Map<String, Object> body = new HashMap<>();
        body.put("error", error);
        SkyflowException e = BaseVaultClient.wrapApiException(400, new RuntimeException("boom"), headers(), body,
                ErrorLogs.INSERT_RECORDS_REJECTED);
        Assert.assertEquals("Bad Request", e.getHttpStatus());
        Assert.assertEquals(Integer.valueOf(3), e.getGrpcCode());
        Assert.assertEquals("Insert failed. Column cv is invalid.", e.getMessage());
    }

    @Test
    public void nullAndStringBodiesDoNotThrow() {
        Assert.assertNull(BaseVaultClient.errorBodyAsJson(null));
        Assert.assertEquals("{\"error\":{}}", BaseVaultClient.errorBodyAsJson("{\"error\":{}}"));
    }
}
