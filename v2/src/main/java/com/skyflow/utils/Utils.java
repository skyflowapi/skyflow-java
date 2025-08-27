package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.vault.connection.InvokeConnectionRequest;

import java.util.HashMap;
import java.util.Map;

public final class Utils extends BaseUtils {

    public static String constructConnectionURL(ConnectionConfig config, InvokeConnectionRequest invokeConnectionRequest) {
        StringBuilder filledURL = new StringBuilder(config.getConnectionUrl());

        if (invokeConnectionRequest.getPathParams() != null && !invokeConnectionRequest.getPathParams().isEmpty()) {
            for (Map.Entry<String, String> entry : invokeConnectionRequest.getPathParams().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                filledURL = new StringBuilder(filledURL.toString().replace(String.format("{%s}", key), value));
            }
        }

        if (invokeConnectionRequest.getQueryParams() != null && !invokeConnectionRequest.getQueryParams().isEmpty()) {
            filledURL.append("?");
            for (Map.Entry<String, String> entry : invokeConnectionRequest.getQueryParams().entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                filledURL.append(key).append("=").append(value).append("&");
            }
            filledURL = new StringBuilder(filledURL.substring(0, filledURL.length() - 1));
        }

        return filledURL.toString();
    }

    public static Map<String, String> constructConnectionHeadersMap(Map<String, String> requestHeaders) {
        Map<String, String> headersMap = new HashMap<>();
        for (Map.Entry<String, String> entry : requestHeaders.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            headersMap.put(key.toLowerCase(), value);
        }
        return headersMap;
    }

    public static JsonObject getMetrics() {
        JsonObject details = getCommonMetrics();
        String sdkVersion = Constants.SDK_VERSION;
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        return details;
    }
}
