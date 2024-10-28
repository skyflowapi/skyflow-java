package com.skyflow.logs;

public enum ErrorLogs {
    // Client initialization
    VAULT_CONFIG_EXISTS("Vault config with vault ID %s1 already exists."),
    VAULT_CONFIG_DOES_NOT_EXIST("Vault config with vault ID %s1 doesn't exist."),
    VAULT_ID_IS_REQUIRED("Invalid vault config. Vault ID is required."),
    EMPTY_VAULT_ID("Invalid vault config. Vault ID can not be empty."),
    CLUSTER_ID_IS_REQUIRED("Invalid vault config. Cluster ID is required."),
    EMPTY_CLUSTER_ID("Invalid vault config. Cluster ID can not be empty."),
    CONNECTION_CONFIG_EXISTS("Connection config with connection ID %s1 already exists."),
    CONNECTION_CONFIG_DOES_NOT_EXIST("Connection config with connection ID %s1 doesn't exist."),
    CONNECTION_ID_IS_REQUIRED("Invalid connection config. Connection ID is required."),
    EMPTY_CONNECTION_ID("Invalid connection config. Connection ID can not be empty."),
    CONNECTION_URL_IS_REQUIRED("Invalid connection config. Connection URL is required."),
    EMPTY_CONNECTION_URL("Invalid connection config. Connection URL can not be empty."),
    INVALID_CONNECTION_URL("Invalid connection config. Connection URL is not a valid URL."),
    MULTIPLE_TOKEN_GENERATION_MEANS_PASSED("Invalid credentials. Only one of 'path', 'credentialsString', 'token' or 'apiKey' is allowed."),
    NO_TOKEN_GENERATION_MEANS_PASSED("Invalid credentials. Any one of 'path', 'credentialsString', 'token' or 'apiKey' is required."),
    EMPTY_CREDENTIALS_PATH("Invalid credentials. Credentials path can not be empty."),
    EMPTY_CREDENTIALS_STRING("Invalid credentials. Credentials string can not be empty."),
    EMPTY_TOKEN_VALUE("Invalid credentials. Token can not be empty."),
    EMPTY_API_KEY_VALUE("Invalid credentials. Api key can not be empty."),
    INVALID_API_KEY("Invalid credentials. Api key is invalid."),
    EMPTY_ROLES("Invalid credentials. Roles can not be empty."),
    EMPTY_OR_NULL_ROLE_IN_ROLES("Invalid credentials. Role can not be null or empty in roles at index %s1."),
    EMPTY_OR_NULL_CONTEXT("Invalid credentials. Context can not be empty."),

    // Bearer token generation
    INVALID_BEARER_TOKEN("Bearer token is invalid or expired."),
    INVALID_CREDENTIALS_FILE("Credentials file is either null or an invalid file."),
    INVALID_CREDENTIALS_STRING("Credentials string is either null or empty."),
    INVALID_CREDENTIALS_FILE_FORMAT("Credentials file is not in a valid JSON format."),
    CREDENTIALS_FILE_NOT_FOUND("Credentials file not found at specified path."),
    INVALID_CREDENTIALS_STRING_FORMAT("Credentials string is not in a valid JSON string format."),
    PRIVATE_KEY_IS_REQUIRED("Private key is required."),
    CLIENT_ID_IS_REQUIRED("Client ID is required."),
    KEY_ID_IS_REQUIRED("Key ID is required."),
    TOKEN_URI_IS_REQUIRED("Token URI is required."),
    INVALID_TOKEN_URI("Invalid value for token URI in credentials."),
    JWT_INVALID_FORMAT("Private key is not in a valid format."),
    INVALID_ALGORITHM("Algorithm for parsing private key is invalid or does not exist."),
    INVALID_KEY_SPEC("Unable to parse RSA private key."),
    BEARER_TOKEN_REJECTED("Bearer token request resulted in failure."),
    SIGNED_DATA_TOKENS_REJECTED("Signed data tokens request resulted in failure."),

    // Vault api interfaces
    TABLE_IS_REQUIRED("Invalid %s1 request. Table is required."),
    EMPTY_TABLE_NAME("Invalid %s1 request. Table name can not be empty."),
    VALUES_IS_REQUIRED("Invalid %s1 request. Values are required."),
    EMPTY_VALUES("Invalid %s1 request. Values can not be empty."),
    EMPTY_OR_NULL_VALUE_IN_VALUES("Invalid %s1 request. Value can not be null or empty in values for key \"%s2\"."),
    EMPTY_OR_NULL_KEY_IN_VALUES("Invalid %s1 request. Key can not be null or empty in values"),
    EMPTY_UPSERT("Invalid %s1 request. Upsert can not be empty."),
    HOMOGENOUS_NOT_SUPPORTED_WITH_UPSERT("Invalid %s1 request. Homogenous is not supported when upsert is passed."),
    TOKENS_NOT_ALLOWED_WITH_BYOT_DISABLE("Invalid %s1 request. Tokens are not allowed when tokenStrict is DISABLE."),
    TOKENS_REQUIRED_WITH_BYOT("Invalid %s1 request. Tokens are required when tokenStrict is %s2."),
    EMPTY_TOKENS("Invalid %s1 request. Tokens can not be empty."),
    EMPTY_OR_NULL_VALUE_IN_TOKENS("Invalid %s1 request. Value can not be null or empty in tokens for key \"%s2\"."),
    EMPTY_OR_NULL_KEY_IN_TOKENS("Invalid %s1 request. Key can not be null or empty in tokens."),
    INSUFFICIENT_TOKENS_PASSED_FOR_BYOT_ENABLE_STRICT("Invalid %s1 request. For tokenStrict as ENABLE_STRICT, tokens should be passed for all fields."),
    MISMATCH_OF_FIELDS_AND_TOKENS("Invalid %s1 request. Keys for values and tokens are not matching."),
    INSERT_RECORDS_REJECTED("Insert request resulted in failure."),
    TOKENS_REQUIRED("Invalid %s1 request. Tokens are required."),
    EMPTY_OR_NULL_TOKEN_IN_TOKENS("Invalid %s1 request. Token can not be null or empty in tokens at index %s2."),
    REDACTION_IS_REQUIRED("Invalid %s1 request. Redaction is required."),
    DETOKENIZE_REQUEST_REJECTED("Detokenize request resulted in failure."),
    IDS_IS_REQUIRED("Invalid %s1 request. Ids are required."),
    EMPTY_IDS("Invalid %s1 request. Ids can not be empty."),
    EMPTY_OR_NULL_ID_IN_IDS("Invalid %s1 request. Id can not be null or empty in ids at index %s2."),
    EMPTY_FIELDS("Invalid %s1 request. Fields can not be empty."),
    EMPTY_OR_NULL_FIELD_IN_FIELDS("Invalid %s1 request. Field can not be null or empty in fields at index %s2."),
    TOKENIZATION_NOT_SUPPORTED_WITH_REDACTION("Invalid %s1 request. Return tokens is not supported when redaction is applied."),
    TOKENIZATION_SUPPORTED_ONLY_WITH_IDS("Invalid %s1 request. Return tokens is not supported when column name and values are passed."),
    EMPTY_OFFSET("Invalid %s1 request. Offset can not be empty."),
    EMPTY_LIMIT("Invalid %s1 request. Limit can not be empty."),
    NEITHER_IDS_NOR_COLUMN_NAME_PASSED("Invalid %s1 request. Neither ids nor column name and values are passed."),
    BOTH_IDS_AND_COLUMN_NAME_PASSED("Invalid %s1 request. Both ids and column name and values are passed."),
    COLUMN_NAME_IS_REQUIRED("Invalid %s1 request. Column name is required when column values are passed."),
    EMPTY_COLUMN_NAME("Invalid %s1 request. Column name can not be empty."),
    COLUMN_VALUES_IS_REQUIRED_GET("Invalid %s1 request. Column values are required when column name is passed."),
    EMPTY_COLUMN_VALUES("Invalid %s1 request. Column values can not be empty."),
    EMPTY_OR_NULL_COLUMN_VALUE_IN_COLUMN_VALUES("Invalid %s1 request. Column value can not by null or empty in column values at index %s2."),
    GET_REQUEST_REJECTED("Get request resulted in failure."),
    SKYFLOW_ID_IS_REQUIRED("Invalid %s1 request. Skyflow Id is required."),
    EMPTY_SKYFLOW_ID("Invalid %s1 request. Skyflow Id can not be empty."),
    UPDATE_REQUEST_REJECTED("Update request resulted in failure."),
    QUERY_IS_REQUIRED("Invalid %s1 request. Query is required."),
    EMPTY_QUERY("Invalid %s1 request. Query can not be empty."),
    QUERY_REQUEST_REJECTED("Query request resulted in failure."),
    COLUMN_VALUES_IS_REQUIRED_TOKENIZE("Invalid %s1 request. ColumnValues are required."),
    EMPTY_OR_NULL_COLUMN_GROUP_IN_COLUMN_VALUES("Invalid %s1 request. Column group can not be null or empty in column values at index %s2."),
    TOKENIZE_REQUEST_REJECTED("Tokenize request resulted in failure."),
    DELETE_REQUEST_REJECTED("Delete request resulted in failure.");

    private final String log;

    ErrorLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
