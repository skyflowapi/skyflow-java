package com.skyflow.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Constants extends BaseConstants {
    public static final String SDK_NAME = "Skyflow Java SDK ";
    public static final String SDK_VERSION;
    public static final String VAULT_DOMAIN = ".skyvault.";
    public static final String SDK_PREFIX;
    public static final Integer INSERT_BATCH_SIZE = 50;
    public static final Integer MAX_INSERT_BATCH_SIZE = 1000;
    public static final Integer INSERT_CONCURRENCY_LIMIT = 1;
    public static final Integer MAX_INSERT_CONCURRENCY_LIMIT = 10;
    public static final Integer DETOKENIZE_BATCH_SIZE = 50;
    public static final Integer DETOKENIZE_CONCURRENCY_LIMIT = 1;
    public static final Integer MAX_DETOKENIZE_BATCH_SIZE = 1000;
    public static final Integer MAX_DETOKENIZE_CONCURRENCY_LIMIT = 10;
    public static final String DEFAULT_SDK_VERSION = "v3";

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
