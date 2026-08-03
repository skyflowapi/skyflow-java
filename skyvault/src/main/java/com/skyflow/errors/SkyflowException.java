package com.skyflow.errors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.utils.Constants;

import java.util.List;
import java.util.Map;

/**
 * Exception thrown by all Skyflow SDK operations.
 *
 * <p>There are two broad categories of errors:
 *
 * <ul>
 *   <li><b>Validation errors</b> — caught before any network call is made (e.g. missing table,
 *       empty token list). These always have {@code httpCode = 400} and an empty
 *       {@link #getDetails()} array. {@link #getRequestId()} and {@link #getGrpcCode()} are
 *       {@code null}.
 *   <li><b>API errors</b> — returned by the Skyflow server. The HTTP status code, gRPC code,
 *       human-readable status string, error message, and request ID are all parsed from the
 *       response and available via the corresponding getters.
 * </ul>
 *
 * <p>Typical error-handling pattern:
 * <pre>{@code
 * try {
 *     InsertResponse response = vault.insert(request);
 * } catch (SkyflowException e) {
 *     System.err.println("HTTP " + e.getHttpCode() + " — " + e.getMessage());
 *     if (e.getRequestId() != null) {
 *         System.err.println("Request ID: " + e.getRequestId());
 *     }
 * }
 * }</pre>
 */
public class SkyflowException extends Exception {
    private String requestId;
    private Integer grpcCode;
    private Integer httpCode;
    private String message;
    private String httpStatus;
    private JsonArray details;
    private JsonObject responseBody;

    public SkyflowException(String message) {
        super(message);
        this.message = message;
    }

    public SkyflowException(Throwable cause) {
        super(cause);
        this.message = cause.getMessage();
    }

    public SkyflowException(String message, Throwable cause) {
        super(message, cause);
        this.message = message;
    }

    /**
     * Constructs a validation error with a fixed HTTP 400 status.
     * {@link #getDetails()} returns an empty array; {@link #getRequestId()} and
     * {@link #getGrpcCode()} return {@code null}.
     */
    public SkyflowException(int code, String message) {
        super(message);
        this.httpCode = code;
        this.message = message;
        this.httpStatus = HttpStatus.BAD_REQUEST.getHttpStatus();
        this.details = new JsonArray();
    }

    /**
     * Constructs an API error from an HTTP response.
     * Parses the JSON error body to populate {@link #getMessage()}, {@link #getGrpcCode()},
     * {@link #getHttpStatus()}, and {@link #getDetails()}. The request ID is read from the
     * {@code x-request-id} response header. If the body cannot be parsed, falls back to the
     * raw body string as the message.
     */
    public SkyflowException(int httpCode, Throwable cause, Map<String, List<String>> responseHeaders, String responseBody) {
        super(cause);
        this.httpCode = httpCode > 0 ? httpCode : 400;
        try {
            setRequestId(responseHeaders);
            setResponseBody(responseBody, responseHeaders);
        } catch (Exception e) {
            this.httpStatus = HttpStatus.BAD_REQUEST.getHttpStatus();
            String fullMessage = responseBody != null ? responseBody :
                    (cause.getLocalizedMessage() != null ? cause.getMessage() : ErrorMessage.ErrorOccurred.getMessage());
            this.message = fullMessage.split("HTTP response code:")[0].trim();
        }
    }

    private void setResponseBody(String responseBody, Map<String, List<String>> responseHeaders) {
        this.responseBody = JsonParser.parseString(responseBody).getAsJsonObject();
        if (this.responseBody.get("error") != null) {
            setGrpcCode();
            setHttpStatus();
            setMessage();
            setDetails(responseHeaders);
        }
    }

    /**
     * Returns the {@code x-request-id} from the server response, useful for support escalations.
     * {@code null} for validation errors that never reached the server.
     */
    public String getRequestId() {
        return requestId;
    }

    private void setRequestId(Map<String, List<String>> responseHeaders) {
        List<String> ids = responseHeaders.get(Constants.REQUEST_ID_HEADER_KEY);
        this.requestId = ids == null ? null : ids.get(0);
    }

    private void setMessage() {
        JsonElement messageElement = ((JsonObject) responseBody.get("error")).get("message");
        this.message = messageElement == null ? null : messageElement.getAsString();
    }

    private void setGrpcCode() {
        JsonElement grpcElement = ((JsonObject) responseBody.get("error")).get("grpc_code");
        this.grpcCode = grpcElement == null ? null : grpcElement.getAsInt();
    }

    private void setHttpStatus() {
        JsonElement statusElement = ((JsonObject) responseBody.get("error")).get("http_status");
        this.httpStatus = statusElement == null ? null : statusElement.getAsString();
    }

    /**
     * Returns the HTTP status code (e.g. 400, 404, 500).
     * Defaults to 400 when the server returned a non-positive code.
     */
    public int getHttpCode() {
        return httpCode;
    }

    /**
     * Returns additional error details from the server response, or an empty array for
     * validation errors. Never {@code null} for validation errors; may be {@code null} for
     * API errors whose response body contained no {@code details} field.
     */
    public JsonArray getDetails() {
        return details;
    }

    private void setDetails(Map<String, List<String>> responseHeaders) {
        JsonElement detailsElement = ((JsonObject) responseBody.get("error")).get("details");
        List<String> errorFromClientHeader = responseHeaders.get(Constants.ERROR_FROM_CLIENT_HEADER_KEY);
        if (detailsElement != null) {
            this.details = detailsElement.getAsJsonArray();
        }
        if (errorFromClientHeader != null) {
            this.details = this.details == null ? new JsonArray() : this.details;
            String errorFromClient = errorFromClientHeader.get(0);
            JsonObject detailObject = new JsonObject();
            detailObject.addProperty("errorFromClient", errorFromClient);
            this.details.add(detailObject);
        }
    }

    /**
     * Returns the gRPC status code from the server response.
     * {@code null} for validation errors and API responses that omit this field.
     */
    public Integer getGrpcCode() {
        return grpcCode;
    }

    /**
     * Returns the human-readable HTTP status string from the server response (e.g.
     * {@code "Bad Request"}, {@code "Not Found"}).
     */
    public String getHttpStatus() {
        return httpStatus;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format(
                "%n requestId: %s%n grpcCode: %s%n httpCode: %s%n httpStatus: %s%n message: %s%n details: %s",
                this.requestId, this.grpcCode, this.httpCode, this.httpStatus, this.message, this.details
        );
    }
}
