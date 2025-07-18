package com.skyflow.logs;

public enum InfoLogs {
    // Client initialization
    CLIENT_INITIALIZED("Initialized skyflow client."),
    VALIDATING_VAULT_CONFIG("Validating vault config."),
    VALIDATING_CONNECTION_CONFIG("Validating connection config."),
    UNABLE_TO_GENERATE_SDK_METRIC("Unable to generate %s1 metric."),
    VAULT_CONTROLLER_INITIALIZED("Initialized vault controller with vault ID %s1."),
    DETECT_CONTROLLER_INITIALIZED("Initialized detect controller with vault ID %s1."),
    CONNECTION_CONTROLLER_INITIALIZED("Initialized connection controller with connection ID %s1."),
    LOGGER_SETUP_DONE("Set up logger."),
    CURRENT_LOG_LEVEL("Current log level is %s1."),

    // Bearer token generation
    EMPTY_BEARER_TOKEN("Bearer token is empty."),
    BEARER_TOKEN_EXPIRED("Bearer token is expired."),
    GET_BEARER_TOKEN_TRIGGERED("getBearerToken method triggered."),
    GET_BEARER_TOKEN_SUCCESS("Bearer token generated."),
    GET_SIGNED_DATA_TOKENS_TRIGGERED("getSignedDataTokens method triggered."),
    GET_SIGNED_DATA_TOKEN_SUCCESS("Signed data tokens generated."),
    REUSE_BEARER_TOKEN("Reusing bearer token."),
    REUSE_API_KEY("Reusing api key."),
    GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_TRIGGERED("generateBearerTokenFromCredentials method triggered."),
    GENERATE_BEARER_TOKEN_FROM_CREDENTIALS_STRING_TRIGGERED("generateBearerTokenFromCredentialString method triggered."),
    GENERATE_SIGNED_TOKENS_FROM_CREDENTIALS_FILE_TRIGGERED("generateSignedTokensFromCredentialsFile method triggered."),
    GENERATE_SIGNED_TOKENS_FROM_CREDENTIALS_STRING_TRIGGERED("generateSignedTokensFromCredentialsString method triggered."),

    // Insert interface
    INSERT_TRIGGERED("Insert method triggered."),
    VALIDATE_INSERT_REQUEST("Validating insert request."),
    INSERT_REQUEST_RESOLVED("Insert request resolved."),
    INSERT_SUCCESS("Data inserted."),

    // Detokenize interface
    DETOKENIZE_TRIGGERED("Detokenize method triggered."),
    VALIDATE_DETOKENIZE_REQUEST("Validating detokenize request."),
    DETOKENIZE_REQUEST_RESOLVED("Detokenize request resolved."),
    DETOKENIZE_PARTIAL_SUCCESS("Data detokenized partially."),
    DETOKENIZE_SUCCESS("Data detokenized."),

    // Get interface
    GET_TRIGGERED("Get method triggered."),
    VALIDATE_GET_REQUEST("Validating get request."),
    GET_REQUEST_RESOLVED("Get request resolved."),
    GET_SUCCESS("Data revealed."),

    // Update interface
    UPDATE_TRIGGERED("Update method triggered."),
    VALIDATE_UPDATE_REQUEST("Validating update request."),
    UPDATE_REQUEST_RESOLVED("Update request resolved."),
    UPDATE_SUCCESS("Data updated."),

    // Delete interface
    DELETE_TRIGGERED("Delete method triggered."),
    VALIDATING_DELETE_REQUEST("Validating delete request."),
    DELETE_REQUEST_RESOLVED("Delete request resolved."),
    DELETE_SUCCESS("Data deleted."),

    // Query interface
    QUERY_TRIGGERED("Query method triggered."),
    VALIDATING_QUERY_REQUEST("Validating query request."),
    QUERY_REQUEST_RESOLVED("Query request resolved."),
    QUERY_SUCCESS("Query executed."),

    // Tokenize interface
    TOKENIZE_TRIGGERED("Tokenize method triggered."),
    VALIDATING_TOKENIZE_REQUEST("Validating tokenize request."),
    TOKENIZE_REQUEST_RESOLVED("Tokenize request resolved."),
    TOKENIZE_SUCCESS("Data tokenized."),

    // File upload interface
    FILE_UPLOAD_TRIGGERED("File upload method triggered."),
    VALIDATE_FILE_UPLOAD_REQUEST("Validating file upload request."),
    FILE_UPLOAD_REQUEST_RESOLVED("File upload request resolved."),
    FILE_UPLOAD_SUCCESS("File uploaded successfully."),

    // Invoke connection interface
    INVOKE_CONNECTION_TRIGGERED("Invoke connection method triggered."),
    VALIDATING_INVOKE_CONNECTION_REQUEST("Validating invoke connection request."),
    INVOKE_CONNECTION_REQUEST_RESOLVED("Invoke connection request resolved."),

    // detect
    VALIDATE_DEIDENTIFY_TEXT_REQUEST("Validating deidentify text request."),
    DEIDENTIFY_TEXT_SUCCESS("Text data de-identified."),
    DEIDENTIFY_TEXT_TRIGGERED("DeIdentify text method triggered."),
    DEIDENTIFY_TEXT_REQUEST_RESOLVED("DeIdentify text request resolved."),
    VALIDATE_REIDENTIFY_TEXT_REQUEST("Validating reidentify text request."),
    REIDENTIFY_TEXT_TRIGGERED("ReIdentify text method triggered."),
    REIDENTIFY_TEXT_REQUEST_RESOLVED("ReIdentify text request resolved."),
    DEIDENTIFY_FILE_TRIGGERED("DeIdentify file method triggered."),
    VALIDATE_DEIDENTIFY_FILE_REQUEST("Validating deidentify file request."),
    DEIDENTIFY_FILE_REQUEST_RESOLVED("DeIdentify file request resolved."),
    DEIDENTIFY_FILE_SUCCESS("File deidentified successfully."),
    GET_DETECT_RUN_TRIGGERED("Get detect run method triggered."),
    VALIDATE_GET_DETECT_RUN_REQUEST("Validating get detect run request."),
    REIDENTIFY_TEXT_SUCCESS("Text data re-identified."),
    ;


    private final String log;

    InfoLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
