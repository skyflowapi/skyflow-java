package com.skyflow.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class HttpUtility {

    private static final String LINE_FEED = "\r\n";
    // Per-thread so concurrent requests do not race on a single shared slot.
    private static final ThreadLocal<String> REQUEST_ID = new ThreadLocal<>();

    public static String getRequestID() {
        return REQUEST_ID.get();
    }

    public static String sendRequest(String method, URL url, JsonObject params, Map<String, String> headers) throws IOException, SkyflowException {

        HttpURLConnection connection = null;
        BufferedReader in = null;
        StringBuilder response = null;
        String boundary = String.valueOf(System.currentTimeMillis());

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty(Constants.HttpUtilityExtra.HEADER_ACCEPT, Constants.HttpUtilityExtra.ACCEPT_ALL);
            boolean hasContentType = headers != null && headers.containsKey(Constants.HttpUtilityExtra.HEADER_CONTENT_TYPE);
            if (!hasContentType && params != null && !params.isEmpty()) {
                connection.setRequestProperty(Constants.HttpUtilityExtra.HEADER_CONTENT_TYPE, Constants.HttpUtilityExtra.CONTENT_TYPE_JSON);
            }

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet()) {
                    connection.setRequestProperty(entry.getKey(), entry.getValue());
                }

                // append dynamic boundary if content-type is multipart/form-data
                if (Constants.HttpUtilityExtra.CONTENT_TYPE_MULTIPART.equals(headers.get(Constants.HttpUtilityExtra.HEADER_CONTENT_TYPE))) {
                    connection.setRequestProperty(Constants.HttpUtilityExtra.HEADER_CONTENT_TYPE,
                            Constants.HttpUtilityExtra.CONTENT_TYPE_MULTIPART + "; boundary=" + boundary);
                }
            }
            if (params != null && !params.isEmpty()) {
                connection.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = encodeRequestBody(params, connection.getRequestProperty(Constants.HttpUtilityExtra.HEADER_CONTENT_TYPE), boundary);
                    wr.write(input, 0, input.length);
                    wr.flush();
                }
            }

            int httpCode = connection.getResponseCode();
            String responseRequestId = connection.getHeaderField(Constants.HttpUtilityExtra.HEADER_REQUEST_ID);
            REQUEST_ID.set(responseRequestId != null ? responseRequestId.split(",")[0] : null);
            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            Reader streamReader;
            if (httpCode > Constants.HttpUtilityExtra.HTTP_SUCCESS_STATUS_MAX) {
                if (connection.getErrorStream() != null) {
                    streamReader = new InputStreamReader(connection.getErrorStream());
                } else {
                    String description = appendRequestId(ErrorMessage.ErrorOccurred.getMessage(), REQUEST_ID.get());
                    throw new SkyflowException(httpCode, new Throwable(description), responseHeaders, Constants.HttpUtilityExtra.EMPTY_JSON_BODY);
                }
            } else {
                streamReader = new InputStreamReader(connection.getInputStream());
            }

            response = new StringBuilder();
            in = new BufferedReader(streamReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            if (httpCode > Constants.HttpUtilityExtra.HTTP_SUCCESS_STATUS_MAX) {
                throw new SkyflowException(httpCode, new Throwable(), responseHeaders, response.toString());
            }
        } finally {
            if (in != null) {
                in.close();
            }
            if (connection != null) {
                connection.disconnect();
            }
        }
        return response.toString();

    }

    private static byte[] encodeRequestBody(JsonObject params, String requestContentType, String boundary) {
        if (requestContentType != null && requestContentType.contains(Constants.HttpUtilityExtra.CONTENT_TYPE_FORM_URLENCODED)) {
            return formatJsonToFormEncodedString(params).getBytes(StandardCharsets.UTF_8);
        } else if (requestContentType != null && requestContentType.contains(Constants.HttpUtilityExtra.CONTENT_TYPE_MULTIPART)) {
            return formatJsonToMultiPartFormDataString(params, boundary).getBytes(StandardCharsets.UTF_8);
        } else {
            return params.toString().getBytes(StandardCharsets.UTF_8);
        }
    }

    public static String formatJsonToFormEncodedString(JsonObject requestBody) {
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String, String> jsonMap = convertJsonToMap(requestBody, "");

        for (Map.Entry<String, String> currentEntry : jsonMap.entrySet()) {
            formEncodeString.append(makeFormEncodeKeyValuePair(currentEntry.getKey(), currentEntry.getValue()));
        }

        return formEncodeString.length() == 0 ? "" : formEncodeString.substring(0, formEncodeString.length() - 1);
    }

    public static String formatJsonToMultiPartFormDataString(JsonObject requestBody, String boundary) {
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String, String> jsonMap = convertJsonToMap(requestBody, "");

        for (Map.Entry<String, String> currentEntry : jsonMap.entrySet()) {
            formEncodeString.append(makeFormDataKeyValuePair(currentEntry.getKey(), currentEntry.getValue(), boundary));
        }

        formEncodeString.append(LINE_FEED);
        formEncodeString.append("--").append(boundary).append("--").append(LINE_FEED);

        return formEncodeString.toString();
    }

    private static HashMap<String, String> convertJsonToMap(JsonObject json, String rootKey) {
        HashMap<String, String> currentMap = new HashMap<>();
        Map<String, JsonElement> jsonMap = json.asMap();
        for (String key : jsonMap.keySet()) {
            JsonElement currentValue = jsonMap.get(key);
            String currentKey = !rootKey.isEmpty() ? rootKey + '[' + key + ']' : rootKey + key;
            if (currentValue.isJsonObject()) {
                currentMap.putAll(convertJsonToMap((JsonObject) currentValue, currentKey));
            } else {
                currentMap.put(currentKey, currentValue.getAsString());
            }
        }
        return currentMap;
    }

    private static String makeFormDataKeyValuePair(String key, String value, String boundary) {
        StringBuilder formDataTextField = new StringBuilder();
        formDataTextField.append("--").append(boundary).append(LINE_FEED);
        formDataTextField.append("Content-Disposition: form-data; name=\"").append(key).append("\"").append(LINE_FEED);
        formDataTextField.append(LINE_FEED);
        formDataTextField.append(value).append(LINE_FEED);

        return formDataTextField.toString();
    }

    public static String appendRequestId(String message, String requestId) {
        if (requestId != null && !requestId.isEmpty()) {
            return message + " - requestId: " + requestId;
        }
        return message;
    }

    private static String makeFormEncodeKeyValuePair(String key, String value) {
        String encodedKey = URLEncoder.encode(key, StandardCharsets.UTF_8);
        String encodedValue = URLEncoder.encode(value, StandardCharsets.UTF_8);
        return encodedKey + "=" + encodedValue + "&";
    }

}
