package com.skyflow.errors;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;

import java.util.List;
import java.util.Map;

public class SkyflowException extends Exception {
    private String requestId;
    private int code;
    private String message;
    private JsonArray details;
    private JsonObject responseBody;

    public SkyflowException() {
    }

    public SkyflowException(String message) {
        super(message);
        this.message = message;
    }

    public SkyflowException(Throwable cause) {
        super(cause);
    }

    public SkyflowException(String message, Throwable cause) {
        super(message, cause);
    }

    public SkyflowException(int code, String message) {
        super(message);
        this.code = code;
    }

    public SkyflowException(int code, Throwable cause, Map<String, List<String>> responseHeaders, String responseBody) {
        this(cause);
        this.code = code;
        setRequestId(responseHeaders);
        setResponseBody(responseBody);
    }

    private void setResponseBody(String responseBody) {
        try {
            if (responseBody != null) {
                this.responseBody = JsonParser.parseString(responseBody).getAsJsonObject();
                setMessage();
                setDetails();
            }
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    public String getRequestId() {
        return requestId;
    }

    private void setRequestId(Map<String, List<String>> responseHeaders) {
        if (responseHeaders != null) {
            List<String> ids = responseHeaders.get("x-request-id");
            this.requestId = ids.get(0);
        }
    }

    private void setMessage() {
        this.message = ((JsonObject) responseBody.get("error")).get("message").getAsString();
    }

    private void setDetails() {
        this.details = ((JsonObject) responseBody.get("error")).get("details").getAsJsonArray();
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return String.format(
                "%n requestId: %s%n code: %s%n message: %s%n details: %s",
                this.requestId, this.code, this.message, this.details
        );
    }

    public JsonArray getDetails() {
        return details;
    }
}
