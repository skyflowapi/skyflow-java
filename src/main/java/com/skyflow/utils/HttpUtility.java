package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.skyflow.errors.SkyflowException;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public final class HttpUtility {

    private static String requestID;

    public static String getRequestID() {
        return requestID;
    }

    private static final String LINE_FEED = "\r\n";

    public static String sendRequest(String method, URL url, JsonObject params, Map<String, String> headers) throws IOException, SkyflowException {

        HttpURLConnection connection = null;
        BufferedReader in = null;
        StringBuffer response = null;
        String boundary = String.valueOf(System.currentTimeMillis());

        try {
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(method);
            connection.setRequestProperty("content-type", "application/json");
            connection.setRequestProperty("Accept", "*/*");

            if (headers != null && headers.size() > 0) {
                for (Map.Entry<String, String> entry : headers.entrySet())
                    connection.setRequestProperty(entry.getKey(), entry.getValue());

                // append dynamic boundary if content-type is multipart/form-data
                if (headers.containsKey("content-type")) {
                    if (headers.get("content-type") == "multipart/form-data") {
                        connection.setRequestProperty("content-type", "multipart/form-data; boundary=" + boundary);
                    }
                }
            }
            if (params != null && params.size() > 0) {
                connection.setDoOutput(true);
                try (DataOutputStream wr = new DataOutputStream(connection.getOutputStream())) {
                    byte[] input = null;
                    String requestContentType = connection.getRequestProperty("content-type");

                    if (requestContentType.contains("application/x-www-form-urlencoded")) {
                        input = formatJsonToFormEncodedString(params).getBytes(StandardCharsets.UTF_8);
                    } else if (requestContentType.contains("multipart/form-data")) {
                        input = formatJsonToMultiPartFormDataString(params, boundary).getBytes(StandardCharsets.UTF_8);
                    }else {
                        input = params.toString().getBytes(StandardCharsets.UTF_8);
                    }

                    wr.write(input, 0, input.length);
                    wr.flush();
                }
            }

            int status = connection.getResponseCode();
            String requestID = connection.getHeaderField("x-request-id");
            HttpUtility.requestID = requestID;

            Reader streamReader;
            if (status > 299) {
                if (connection.getErrorStream() != null)
                    streamReader = new InputStreamReader(connection.getErrorStream());
                else {
                    String description = appendRequestId("replace with description", requestID);
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

            if (status > 299) {
                String errorMsg = appendRequestIdToErrorObj(status, response.toString(), requestID);
                throw new SkyflowException(errorMsg);
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
        formEncodeString.append("--").append(boundary).append("--").append(LINE_FEED);

        return formEncodeString.toString();
    }

    private static HashMap<String, String> convertJsonToMap(JsonObject json, String rootKey) {
        HashMap<String, String> currentMap = new HashMap<>();
        for (Object key : json.keySet()) {
            Object currentValue = json.get((String) key);
            String currentKey = rootKey.length() != 0 ? rootKey + '[' + key.toString() + ']' : rootKey + key.toString();
            if (currentValue instanceof JsonObject) {
                currentMap.putAll(convertJsonToMap((JsonObject) currentValue, currentKey));
            } else {
                currentMap.put(currentKey, currentValue.toString());
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
            message = message + " - requestId: " + requestId;
        }
        return message;
    }

    public static String appendRequestIdToErrorObj(int status, String error, String requestId) throws SkyflowException {
        if (requestId != null && !requestId.isEmpty()) {
            JsonObject errorObject = (JsonObject) new JsonParser().parse(error);
            JsonObject tempError = (JsonObject) errorObject.get("error");
            if (tempError != null) {
                String message = String.valueOf(tempError.get("message"));
                message = message + " - requestId: " + requestId;

                tempError.addProperty("message", message);
                errorObject.add("error", tempError);
            }
            error = errorObject.toString();
        }
        return error;
    }

    private static String makeFormEncodeKeyValuePair(String key, String value) {
        return key + "=" + value + "&";
    }

}
