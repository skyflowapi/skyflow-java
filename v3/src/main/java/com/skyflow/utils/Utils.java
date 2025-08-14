package com.skyflow.utils;

import com.google.gson.JsonObject;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;

public final class Utils extends BaseUtils {

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
