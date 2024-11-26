package com.skyflow.utils;

public final class Constants {
    public static final String SECURE_PROTOCOL = "https://";
    public static final String DEV_DOMAIN = ".vault.skyflowapis.dev";
    public static final String STAGE_DOMAIN = ".vault.skyflowapis.tech";
    public static final String SANDBOX_DOMAIN = ".vault.skyflowapis-preview.com";
    public static final String PROD_DOMAIN = ".vault.skyflowapis.com";
    public static final String PKCS8_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    public static final String PKCS8_PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String SIGNED_DATA_TOKEN_PREFIX = "signed_token_";
    public static final String ORDER_ASCENDING = "ASCENDING";
    public static final String API_KEY_REGEX = "^sky-[a-zA-Z0-9]{5}-[a-fA-F0-9]{32}$";
    public static final String ENV_CREDENTIALS_KEY_NAME = "SKYFLOW_CREDENTIALS";
    public static final String SDK_NAME = "Skyflow Java SDK ";
    public static final String SDK_VERSION = "v2";
    public static final String SDK_PREFIX = SDK_NAME + SDK_VERSION;
}
