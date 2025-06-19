package com.skyflow.errors;

import com.google.gson.*;

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
        this.httpCode = httpCode;
        setRequestId(responseHeaders);
        String errorObject = parseJsonStringWithErrorObject(responseBody);
        if (!errorObject.isEmpty()) {
            setResponseBodyFromJson(errorObject, responseHeaders);
        } else {
            this.message = errorObject;
            this.details = new JsonArray();
        }
    }

    private String parseJsonStringWithErrorObject(String responseBody) {
        try {
            if (responseBody == null || responseBody.trim().isEmpty()) return "";

            // If already valid JSON, parsing it directly
            try {
                JsonObject obj = JsonParser.parseString(responseBody).getAsJsonObject();
                // If it's valid JSON and has error object, return as is
                if (obj.has("error")) {
                    return responseBody;
                }
                return "";
            } catch (JsonSyntaxException e) {
                // If not valid JSON, continue with Java object parsing
            }

            // Handle Java object notation
            if (!responseBody.contains("error={")) {
                return "";
            }

            // Handle Java object string representation
            StringBuilder json = new StringBuilder("{");
            if (responseBody.contains("error={")) {
                String content = responseBody.substring(1, responseBody.length() - 1);
                if (content.startsWith("error={")) {
                    json.append("\"error\":{");
                    String errorContent = content.substring(7, content.length() - 1);

                    // Process key-value pairs
                    String[] pairs = errorContent.split(", ");
                    for (int i = 0; i < pairs.length; i++) {
                        String[] keyValue = pairs[i].split("=", 2);
                        json.append("\"").append(keyValue[0]).append("\":");

                        if (keyValue[1].equals("[]")) {
                            json.append("[]");
                        } else if (keyValue[1].matches("\\d+")) {
                            json.append(keyValue[1]);
                        } else {
                            json.append("\"").append(keyValue[1].replace("\"", "\\\"")).append("\"");
                        }

                        if (i < pairs.length - 1) {
                            json.append(",");
                        }
                    }
                    json.append("}");
                }
            }
            json.append("}");

            String validJson = json.toString();
            JsonObject obj = JsonParser.parseString(validJson).getAsJsonObject();
            return obj.has("error") && obj.get("error").isJsonObject() ? validJson : "";
        } catch (JsonSyntaxException e) {
            throw new RuntimeException(e);
        }
    }

    private void setResponseBodyFromJson(String responseBody, Map<String, List<String>> responseHeaders) {
        try {
            if (responseBody != null) {
                this.responseBody = JsonParser.parseString(responseBody).getAsJsonObject();
                if (this.responseBody.has("error")) {
                    JsonObject errorObj = this.responseBody.getAsJsonObject("error");
                    setGrpcCode(errorObj);
                    setHttpStatus(errorObj);
                    setMessage(errorObj);
                    setDetails(errorObj, responseHeaders);
                }
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
            this.requestId = ids == null ? null : ids.get(0);
        }
    }

    // For legacy error structure
    private void setMessage() {
        JsonElement messageElement = ((JsonObject) responseBody.get("error")).get("message");
        this.message = messageElement == null ? null : messageElement.getAsString();
    }

    // For new error structure
    private void setMessage(JsonObject errorObj) {
        JsonElement messageElement = errorObj.get("message");
        this.message = messageElement == null ? null : messageElement.getAsString();
    }

    // For legacy error structure
    private void setGrpcCode() {
        JsonElement grpcElement = ((JsonObject) responseBody.get("error")).get("grpc_code");
        this.grpcCode = grpcElement == null ? null : grpcElement.getAsInt();
    }

    // For new error structure
    private void setGrpcCode(JsonObject errorObj) {
        JsonElement grpcElement = errorObj.get("grpc_code");
        this.grpcCode = grpcElement == null ? null : grpcElement.getAsInt();
    }

    // For legacy error structure
    private void setHttpStatus() {
        JsonElement statusElement = ((JsonObject) responseBody.get("error")).get("http_status");
        this.httpStatus = statusElement == null ? null : statusElement.getAsString();
    }

    // For new error structure
    private void setHttpStatus(JsonObject errorObj) {
        JsonElement statusElement = errorObj.get("http_status");
        this.httpStatus = statusElement == null ? null : statusElement.getAsString();
    }

    // For legacy error structure
    private void setDetails(Map<String, List<String>> responseHeaders) {
        JsonElement detailsElement = ((JsonObject) responseBody.get("error")).get("details");
        List<String> errorFromClientHeader = responseHeaders.get("error-from-client");
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

    // For new error structure
    private void setDetails(JsonObject errorObj, Map<String, List<String>> responseHeaders) {
        JsonElement detailsElement = errorObj.get("details");
        List<String> errorFromClientHeader = responseHeaders.get("error-from-client");
        if (detailsElement != null && detailsElement.isJsonArray()) {
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

    public int getHttpCode() {
        return httpCode;
    }

    public JsonArray getDetails() {
        return details;
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
