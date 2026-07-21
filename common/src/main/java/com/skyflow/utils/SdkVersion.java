package com.skyflow.utils;

public class SdkVersion {
    private static String sdkPrefix = BaseConstants.SDK_PREFIX;

    public static String getSdkPrefix() {
        return sdkPrefix;
    }

    public static void setSdkPrefix(String sdkPrefix) {
        SdkVersion.sdkPrefix = sdkPrefix;
    }
}
