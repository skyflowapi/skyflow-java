package com.skyflow.utils;

public final class Constants extends BaseConstants {
    public static final String SDK_NAME = "Skyflow Java SDK ";
    public static final String SDK_VERSION = "3.0.0-beta.0";
    public static final String VAULT_DOMAIN = ".skyvault.";
    public static final String SDK_PREFIX = SDK_NAME + SDK_VERSION;
    public static final Integer INSERT_BATCH_SIZE = 50;
    public static final Integer MAX_INSERT_BATCH_SIZE = 1000;
    public static final Integer INSERT_CONCURRENCY_LIMIT = 10;
    public static final Integer MAX_INSERT_CONCURRENCY_LIMIT = 10;
    public static final Integer DETOKENIZE_BATCH_SIZE = 50;
    public static final Integer DETOKENIZE_CONCURRENCY_LIMIT = 10;

    public static final Integer MAX_DETOKENIZE_BATCH_SIZE = 1000;
    public static final Integer MAX_DETOKENIZE_CONCURRENCY_LIMIT = 10;

}
