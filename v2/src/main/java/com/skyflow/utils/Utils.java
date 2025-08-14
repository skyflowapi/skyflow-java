package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.config.ConnectionConfig;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
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
        JsonObject details = new JsonObject();
        String sdkVersion = Constants.SDK_VERSION;
        String deviceModel;
        String osDetails;
        String javaVersion;
        // Retrieve device model
        try {
            deviceModel = System.getProperty("os.name");
            if (deviceModel == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_DEVICE_MODEL
            ));
            deviceModel = "";
        }

        // Retrieve OS details
        try {
            osDetails = System.getProperty("os.version");
            if (osDetails == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_CLIENT_OS_DETAILS
            ));
            osDetails = "";
        }

        // Retrieve Java version details
        try {
            javaVersion = System.getProperty("java.version");
            if (javaVersion == null) throw new Exception();
        } catch (Exception e) {
            LogUtil.printInfoLog(parameterizedString(
                    InfoLogs.UNABLE_TO_GENERATE_SDK_METRIC.getLog(),
                    Constants.SDK_METRIC_RUNTIME_DETAILS
            ));
            javaVersion = "";
        }
        details.addProperty(Constants.SDK_METRIC_NAME_VERSION, Constants.SDK_METRIC_NAME_VERSION_PREFIX + sdkVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_DEVICE_MODEL, deviceModel);
        details.addProperty(Constants.SDK_METRIC_RUNTIME_DETAILS, Constants.SDK_METRIC_RUNTIME_DETAILS_PREFIX + javaVersion);
        details.addProperty(Constants.SDK_METRIC_CLIENT_OS_DETAILS, osDetails);
        return details;
    }
}
