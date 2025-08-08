package com.skyflow.common.utils;

import com.skyflow.v2.utils.Constants;

public class SdkVersion {
    private static String sdkPrefix = Constants.SDK_PREFIX;

    public static void setSdkPrefix(String prefix) {
        sdkPrefix = prefix;
    }

    public static String getSdkPrefix() {
        return sdkPrefix;
    }
}

