package com.skyflow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Constants extends BaseConstants {
    public static final String SDK_NAME = "Skyflow Java SDK";
    public static final String DEFAULT_SDK_VERSION = "v2";
    public static final String SDK_VERSION;
    public static final String SDK_PREFIX;
    public static final String SDK_METRIC_NAME_VERSION_PREFIX = "skyflow-java@";
    public static final String PROCESSED_FILE_NAME_PREFIX = "processed-";
    public static final String DEIDENTIFIED_FILE_PREFIX = "deidentified";
    public static final String HTTPS_PROTOCOL = "https";
    public static final String CURLY_PLACEHOLDER = "{%s}";
    public static final String EMPTY_STRING = "";
    public static final String QUOTE = "\"";

    public static final class HttpUtilityExtra {
        public static final String SDK_GENERATED_PREFIX = "SDK-Generated-";
        private HttpUtilityExtra() {}
    }

    static {
        String sdkVersion;
        // Use a static initializer block to read the properties file
        Properties properties = new Properties();
        try (InputStream input = Constants.class.getClassLoader().getResourceAsStream("sdk.properties")) {
            if (input == null) {
                sdkVersion = DEFAULT_SDK_VERSION;
            } else {
                properties.load(input);
                sdkVersion = properties.getProperty("sdk.version", DEFAULT_SDK_VERSION);
            }
        } catch (IOException ex) {
            sdkVersion = DEFAULT_SDK_VERSION;
        }
        SDK_VERSION = sdkVersion;
        SDK_PREFIX = SDK_NAME + " " + SDK_VERSION;
    }
}
