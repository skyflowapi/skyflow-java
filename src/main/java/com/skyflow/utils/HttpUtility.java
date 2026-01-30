package com.skyflow.utils;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.skyflow.errors.SkyflowException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class HttpUtility {

    private static final String LINE_FEED = "\r\n";
    private static String requestID;

    public static String getRequestID() {
        return requestID;
    }

    public static String sendRequest(String method, URL url, JsonObject params, Map<String, String> headers) throws IOException, SkyflowException {

        HttpURLConnection connection = null;
        BufferedReader in = null;
        StringBuffer response = null;
        String boundary = String.valueOf(System.currentTimeMillis());

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty(Constants.HttpHeader.CONTENT_TYPE, Constants.HttpHeader.CONTENT_TYPE_JSON);
            connection.setRequestProperty(Constants.HttpHeader.ACCEPT, Constants.HttpHeader.ACCEPT_ALL);

            if (headers != null && !headers.isEmpty()) {
                for (Map.Entry<String, String> entry : headers.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue());

                // append dynamic boundary if content-type is multipart/form-data
                if (headers.containsKey(Constants.HttpHeader.CONTENT_TYPE)) {
                    if (Objects.equals(headers.get(Constants.HttpHeader.CONTENT_TYPE), Constants.HttpHeader.CONTENT_TYPE_MULTIPART)) {
                        connection.setRequestProperty(Constants.HttpHeader.CONTENT_TYPE, Constants.HttpHeader.CONTENT_TYPE_MULTIPART + Constants.HttpHeader.BOUNDARY_SEPARATOR + boundary);
                    }
                }
            }
            if (params != null && !params.isEmpty()) {
                connection.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = null;
                    String requestContentType = connection.getRequestProperty(Constants.HttpHeader.CONTENT_TYPE);

                    if (requestContentType.contains(Constants.HttpHeader.CONTENT_TYPE_FORM_URLENCODED)) {
                        input = formatJsonToFormEncodedString(params).getBytes(StandardCharsets.UTF_8);
                    } else if (requestContentType.contains(Constants.HttpHeader.CONTENT_TYPE_MULTIPART)) {
                        input = formatJsonToMultiPartFormDataString(params, boundary).getBytes(StandardCharsets.UTF_8);
                    } else {
                        input = params.toString().getBytes(StandardCharsets.UTF_8);
                    }

                    wr.write(input, 0, input.length);
                    wr.flush();
                }
            }

            int httpCode = connection.getResponseCode();
            String requestID = connection.getHeaderField(Constants.REQUEST_ID_HEADER_KEY);
            HttpUtility.requestID = requestID.split(Constants.HttpUtility.REQUEST_ID_DELIMITER)[0];
            Map<String, List<String>> responseHeaders = connection.getHeaderFields();
            Reader streamReader;
            if (httpCode > 299) {
                if (connection.getErrorStream() != null)
                    streamReader = new InputStreamReader(connection.getErrorStream());
                else {
                    String description = appendRequestId(Constants.HttpUtility.ERROR_DESCRIPTION, requestID);
                    throw new SkyflowException(description);
                }
            } else {
                streamReader = new InputStreamReader(connection.getInputStream());
            }

            response = new StringBuffer();
            in = new BufferedReader(streamReader);
            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                response.append(inputLine);
            }

            if (httpCode > 299) {
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

    public static String formatJsonToFormEncodedString(JsonObject requestBody) {
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String, String> jsonMap = convertJsonToMap(requestBody, "");

        for (Map.Entry<String, String> currentEntry : jsonMap.entrySet())
            formEncodeString.append(makeFormEncodeKeyValuePair(currentEntry.getKey(), currentEntry.getValue()));

        return formEncodeString.substring(0, formEncodeString.length() - 1);
    }

    public static String formatJsonToMultiPartFormDataString(JsonObject requestBody, String boundary) {
        StringBuilder formEncodeString = new StringBuilder();
        HashMap<String, String> jsonMap = convertJsonToMap(requestBody, "");

        for (Map.Entry<String, String> currentEntry : jsonMap.entrySet())
            formEncodeString.append(makeFormDataKeyValuePair(currentEntry.getKey(), currentEntry.getValue(), boundary));

        formEncodeString.append(LINE_FEED);
        formEncodeString.append(Constants.FormData.BOUNDARY_SEPARATOR).append(boundary).append(Constants.FormData.BOUNDARY_SEPARATOR).append(LINE_FEED);

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
        formDataTextField.append(Constants.FormData.BOUNDARY_SEPARATOR).append(boundary).append(LINE_FEED);
        formDataTextField.append(Constants.HttpHeader.CONTENT_DISPOSITION).append(Constants.HttpHeader.FORM_DATA_HEADER).append(key).append("\"").append(LINE_FEED);
        formDataTextField.append(LINE_FEED);
        formDataTextField.append(value).append(LINE_FEED);

        return formDataTextField.toString();
    }

    public static String appendRequestId(String message, String requestId) {
        if (requestId != null && !requestId.isEmpty()) {
            message = message + Constants.HttpUtility.REQUEST_ID_PREFIX + requestId;
        }
        return message;
    }

    private static String makeFormEncodeKeyValuePair(String key, String value) {
        return key + Constants.HttpUtility.FORM_ENCODE_SEPARATOR + value + Constants.HttpUtility.FORM_ENCODE_DELIMITER;
    }

}
