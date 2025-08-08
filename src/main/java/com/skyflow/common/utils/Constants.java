package com.skyflow.common.utils;

public class Constants {
    public static final String ERROR_FROM_CLIENT_HEADER_KEY = "error-from-client";
    public static final String REQUEST_ID_HEADER_KEY = "x-request-id";
    public static final String GRANT_TYPE = "urn:ietf:params:oauth:grant-type:jwt-bearer";
    public static final String SECURE_PROTOCOL = "https://";
    public static final String SIGNED_DATA_TOKEN_PREFIX = "signed_token_";

    public static final String API_KEY_REGEX = "^sky-[a-zA-Z0-9]{5}-[a-fA-F0-9]{32}$";
    public static final String DEV_DOMAIN = ".vault.skyflowapis.dev";
    public static final String STAGE_DOMAIN = ".vault.skyflowapis.tech";
    public static final String SANDBOX_DOMAIN = ".vault.skyflowapis-preview.com";
    public static final String PROD_DOMAIN = ".vault.skyflowapis.com";
    public static final String PKCS8_PRIVATE_HEADER = "-----BEGIN PRIVATE KEY-----";
    public static final String PKCS8_PRIVATE_FOOTER = "-----END PRIVATE KEY-----";
    public static final String ENV_CREDENTIALS_KEY_NAME = "SKYFLOW_CREDENTIALS";

}
