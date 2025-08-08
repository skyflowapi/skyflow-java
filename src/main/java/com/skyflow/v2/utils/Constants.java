package com.skyflow.v2.utils;

public final class Constants extends com.skyflow.common.utils.Constants {
    public static final String ORDER_ASCENDING = "ASCENDING";
    public static final String ENV_CREDENTIALS_KEY_NAME = "SKYFLOW_CREDENTIALS";
    public static final String SDK_NAME = "Skyflow Java SDK ";
    public static final String SDK_VERSION = "v2";

    public static final String SDK_PREFIX = SDK_NAME + SDK_VERSION;
    public static final String SDK_METRIC_NAME_VERSION = "sdk_name_version";
    public static final String SDK_METRIC_NAME_VERSION_PREFIX = "skyflow-java@";
    public static final String SDK_METRIC_CLIENT_DEVICE_MODEL = "sdk_client_device_model";
    public static final String SDK_METRIC_CLIENT_OS_DETAILS = "sdk_client_os_details";
    public static final String SDK_METRIC_RUNTIME_DETAILS = "sdk_runtime_details";
    public static final String SDK_METRIC_RUNTIME_DETAILS_PREFIX = "Java@";
    public static final String SDK_AUTH_HEADER_KEY = "x-skyflow-authorization";
    public static final String SDK_METRICS_HEADER_KEY = "sky-metadata";
    public static final String REQUEST_ID_HEADER_KEY = "x-request-id";
    public static final String PROCESSED_FILE_NAME_PREFIX = "processed-";
    public static final String ERROR_FROM_CLIENT_HEADER_KEY = "eror-from-client";
    public static final String DEIDENTIFIED_FILE_PREFIX = "deidentified";
}
