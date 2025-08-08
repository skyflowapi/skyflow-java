package com.skyflow.common.errors;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.common.utils.Constants;

import java.util.List;
import java.util.Map;

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

    public SkyflowException(int code, String message) {
        super(message);
        this.httpCode = code;
        this.message = message;
        this.httpStatus = HttpStatus.BAD_REQUEST.getHttpStatus();
        this.details = new JsonArray();
    }

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

    public int getHttpCode() {
        return httpCode;
    }

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

    public Integer getGrpcCode() {
        return grpcCode;
    }

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
