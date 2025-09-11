package com.skyflow.errors;

import com.skyflow.utils.SdkVersion;

public enum ErrorMessage {
    // Client initialization
    VaultIdAlreadyInConfigList("%s0 Validation error. VaultId is present in an existing config. Specify a new vaultId in config."),
    VaultIdNotInConfigList("%s0 Validation error. VaultId is missing from the config. Specify the vaultIds from configs."),
    OnlySingleVaultConfigAllowed("%s0 Validation error. A vault config already exists. Cannot add another vault config."),
    ConnectionIdAlreadyInConfigList("%s0 Validation error. ConnectionId is present in an existing config. Specify a connectionId in config."),
    ConnectionIdNotInConfigList("%s0 Validation error. ConnectionId is missing from the config. Specify the connectionIds from configs."),
    EmptyCredentials("%s0 Validation error. Invalid credentials. Credentials must not be empty."),

    // Vault config
    InvalidVaultId("%s0 Initialization failed. Invalid vault ID. Specify a valid vault ID."),
    EmptyVaultId("%s0 Initialization failed. Invalid vault ID. Vault ID must not be empty."),
    InvalidClusterId("%s0 Initialization failed. Invalid cluster ID. Specify cluster ID."),
    EmptyClusterId("%s0 Initialization failed. Invalid cluster ID. Specify a valid cluster ID."),
    EmptyVaultUrl("%s0 Initialization failed. Vault URL is empty. Specify a valid vault URL."),
    InvalidVaultUrlFormat("%s0 Initialization failed. Vault URL must start with 'https://'."),

    // Connection config
    InvalidConnectionId("%s0 Initialization failed. Invalid connection ID. Specify a valid connection ID."),
    EmptyConnectionId("%s0 Initialization failed. Invalid connection ID. Connection ID must not be empty."),
    InvalidConnectionUrl("%s0 Initialization failed. Invalid connection URL. Specify a valid connection URL."),
    EmptyConnectionUrl("%s0 Initialization failed. Invalid connection URL. Connection URL must not be empty."),
    InvalidConnectionUrlFormat("%s0 Initialization failed. Connection URL is not a valid URL. Specify a valid connection URL."),

    // Credentials
    MultipleTokenGenerationMeansPassed("%s0 Initialization failed. Invalid credentials. Specify only one from 'path', 'credentialsString', 'token' or 'apiKey'."),
    NoTokenGenerationMeansPassed("%s0 Initialization failed. Invalid credentials. Specify any one from 'path', 'credentialsString', 'token' or 'apiKey'."),
    EmptyCredentialFilePath("%s0 Initialization failed. Invalid credentials. Credentials file path must not be empty."),
    EmptyCredentialsString("%s0 Initialization failed. Invalid credentials. Credentials string must not be empty."),
    EmptyToken("%s0 Initialization failed. Invalid credentials. Token must not be empty."),
    EmptyApikey("%s0 Initialization failed. Invalid credentials. Api key must not be empty."),
    InvalidApikey("%s0 Initialization failed. Invalid credentials. Specify valid api key."),
    EmptyRoles("%s0 Initialization failed. Invalid roles. Specify at least one role."),
    EmptyRoleInRoles("%s0 Initialization failed. Invalid role. Specify a valid role."),
    EmptyContext("%s0 Initialization failed. Invalid context. Specify a valid context."),

    // Bearer token generation
    FileNotFound("%s0 Initialization failed. Credential file not found at %s1. Verify the file path."),
    FileInvalidJson("%s0 Initialization failed. File at %s1 is not in valid JSON format. Verify the file contents."),
    CredentialsStringInvalidJson("%s0 Initialization failed. Credentials string is not in valid JSON format. Verify the credentials string contents."),
    InvalidCredentials("%s0 Initialization failed. Invalid credentials provided. Specify valid credentials."),
    MissingPrivateKey("%s0 Initialization failed. Unable to read private key in credentials. Verify your private key."),
    MissingClientId("%s0 Initialization failed. Unable to read client ID in credentials. Verify your client ID."),
    MissingKeyId("%s0 Initialization failed. Unable to read key ID in credentials. Verify your key ID."),
    MissingTokenUri("%s0 Initialization failed. Unable to read token URI in credentials. Verify your token URI."),
    InvalidTokenUri("%s0 Initialization failed. Token URI in not a valid URL in credentials. Verify your token URI."),
    JwtInvalidFormat("%s0 Initialization failed. Invalid private key format. Verify your credentials."),
    InvalidAlgorithm("%s0 Initialization failed. Invalid algorithm to parse private key. Specify valid algorithm."),
    InvalidKeySpec("%s0 Initialization failed. Unable to parse RSA private key. Verify your credentials."),
    JwtDecodeError("%s0 Validation error. Invalid access token. Verify your credentials."),
    MissingAccessToken("%s0 Validation error. Access token not present in the response from bearer token generation. Verify your credentials."),
    MissingTokenType("%s0 Validation error. Token type not present in the response from bearer token generation. Verify your credentials."),
    BearerTokenExpired("%s0 Validation error. Bearer token is invalid or expired. Please provide a valid bearer token."),

    // Insert
    TableKeyError("%s0 Validation error. 'table' key is missing from the payload. Specify a 'table' key."),
    EmptyTable("%s0 Validation error. 'table' can't be empty. Specify a table."),
    ValuesKeyError("%s0 Validation error. 'values' key is missing from the payload. Specify a 'values' key."),
    EmptyValues("%s0 Validation error. 'values' can't be empty. Specify values."),
    EmptyKeyInValues("%s0 Validation error. Invalid key in values. Specify a valid key."),
    EmptyValueInValues("%s0 Validation error. Invalid value in values. Specify a valid value."),
    TokensKeyError("%s0 Validation error. 'tokens' key is missing from the payload. Specify a 'tokens' key."),
    EmptyTokens("%s0 Validation error. The 'tokens' field is empty. Specify tokens for one or more fields."),
    EmptyKeyInTokens("%s0 Validation error. Invalid key tokens. Specify a valid key."),
    EmptyValueInTokens("%s0 Validation error. Invalid value in tokens. Specify a valid value."),
    EmptyUpsert("%s0 Validation error. 'upsert' key can't be empty. Specify an upsert column."),
    EmptyUpsertValues("%s0 Validation error. Upsert column values can't be empty. Specify at least one upsert column."),
    HomogenousNotSupportedWithUpsert("%s0 Validation error. 'homogenous' is not supported with 'upsert'. Specify either 'homogenous' or 'upsert'."),
    TokensPassedForTokenModeDisable("%s0 Validation error. 'tokenMode' wasn't specified. Set 'tokenMode' to 'ENABLE' to insert tokens."),
    NoTokensWithTokenMode("%s0 Validation error. Tokens weren't specified for records while 'tokenMode' was %s1. Specify tokens."),
    MismatchOfFieldsAndTokens("%s0 Validation error. 'fields' and 'tokens' have different columns names. Verify that 'fields' and 'tokens' columns match."),
    InsufficientTokensPassedForTokenModeEnableStrict("%s0 Validation error. 'tokenMode' is set to 'ENABLE_STRICT', but some fields are missing tokens. Specify tokens for all fields."),
    BatchInsertPartialSuccess("%s0 Insert operation completed with partial success."),
    BatchInsertFailure("%s0 Insert operation failed."),
    RecordSizeExceedError("%s0 Maximum number of records exceeded. The limit is 10000."),

    // Detokenize
    InvalidDetokenizeData("%s0 Validation error. Invalid detokenize data. Specify valid detokenize data."),
    EmptyDetokenizeData("%s0 Validation error. Invalid data tokens. Specify at least one data token."),
    EmptyTokenInDetokenizeData("%s0 Validation error. Invalid data tokens. Specify a valid data token."),
    TokensSizeExceedError("%s0 Maximum number of tokens exceeded. The limit is 10000."),

    // Get
    IdsKeyError("%s0 Validation error. 'ids' key is missing from the payload. Specify an 'ids' key."),
    EmptyIds("%s0 Validation error. 'ids' can't be empty. Specify at least one id."),
    EmptyIdInIds("%s0 Validation error. Invalid id in 'ids'. Specify a valid id."),
    EmptyFields("%s0 Validation error. Fields are empty in get payload. Specify at least one field."),
    EmptyFieldInFields("%s0 Validation error. Invalid field in 'fields'. Specify a valid field."),
    RedactionKeyError("%s0 Validation error. 'redaction' key is missing from the payload. Specify a 'redaction' key."),
    RedactionWithTokensNotSupported("%s0 Validation error. 'redaction' can't be used when 'returnTokens' is specified. Remove 'redaction' from payload if 'returnTokens' is specified."),
    TokensGetColumnNotSupported("%s0 Validation error. Column name and/or column values can't be used when 'returnTokens' is specified. Remove unique column values or 'returnTokens' from the payload."),
    EmptyOffset("%s0 Validation error. 'offset' can't be empty. Specify an offset."),
    EmptyLimit("%s0 Validation error. 'limit' can't be empty. Specify a limit."),
    UniqueColumnOrIdsKeyError("%s0 Validation error. 'ids' or 'columnName' key is missing from the payload. Specify the ids or unique 'columnName' in payload."),
    BothIdsAndColumnDetailsSpecified("%s0 Validation error. Both Skyflow IDs and column details can't be specified. Either specify Skyflow IDs or unique column details."),
    ColumnNameKeyError("%s0 Validation error. 'columnName' isn't specified whereas 'columnValues' are specified. Either add 'columnName' or remove 'columnValues'."),
    EmptyColumnName("%s0 Validation error. 'columnName' can't be empty. Specify a column name."),
    ColumnValuesKeyErrorGet("%s0 Validation error. 'columnValues' aren't specified whereas 'columnName' is specified. Either add 'columnValues' or remove 'columnName'."),
    EmptyColumnValues("%s0 Validation error. 'columnValues' can't be empty. Specify at least one column value"),
    EmptyValueInColumnValues("%s0 Validation error. Invalid value in column values. Specify a valid column value."),

    TokenKeyError("%s0 Validation error. 'token' key is missing from the payload. Specify a 'token' key."),
    PartialSuccess("%s0 Validation error. Check 'SkyflowError.data' for details."),

    // Update
    DataKeyError("%s0 Validation error. 'data' key is missing from the payload. Specify a 'data' key."),
    EmptyData("%s0 Validation error. 'data' can't be empty. Specify data."),
    SkyflowIdKeyError("%s0 Validation error. 'skyflow_id' is missing from the data payload. Specify a 'skyflow_id'."),
    InvalidSkyflowIdType("%s0 Validation error. Invalid type for 'skyflow_id' in data payload. Specify 'skyflow_id' as a string."),
    EmptySkyflowId("%s0 Validation error. 'skyflow_id' can't be empty. Specify a skyflow id."),

    // Query
    QueryKeyError("%s0 Validation error. 'query' key is missing from the payload. Specify a 'query' key."),
    EmptyQuery("%s0 Validation error. 'query' can't be empty. Specify a query"),

    // Tokenize
    ColumnValuesKeyErrorTokenize("%s0 Validation error. 'columnValues' key is missing from the payload. Specify a 'columnValues' key."),
    EmptyColumnGroupInColumnValue("%s0 Validation error. Invalid column group in column value. Specify a valid column group."),

    // Connection
    InvalidRequestHeaders("%s0 Validation error. Request headers aren't valid. Specify valid request headers."),
    EmptyRequestHeaders("%s0 Validation error. Request headers are empty. Specify valid request headers."),
    InvalidPathParams("%s0 Validation error. Path parameters aren't valid. Specify valid path parameters."),
    EmptyPathParams("%s0 Validation error. Path parameters are empty. Specify valid path parameters."),
    InvalidQueryParams("%s0 Validation error. Query parameters aren't valid. Specify valid query parameters."),
    EmptyQueryParams("%s0 Validation error. Query parameters are empty. Specify valid query parameters."),
    InvalidRequestBody("%s0 Validation error. Invalid request body. Specify the request body as an object."),
    EmptyRequestBody("%s0 Validation error. Request body can't be empty. Specify a valid request body."),

    // detect
    InvalidTextInDeIdentify("%s0 Validation error. The text field is required and must be a non-empty string. Specify a valid text."),
    InvalidTextInReIdentify("%s0 Validation error. The text field is required and must be a non-empty string. Specify a valid text."),

    //Detect Files
    InvalidNullFileInDeIdentifyFile("%s0 Validation error. The file field is required and must not be null. Specify a valid file object."),
    InvalidFilePath("%s0 Validation error. The file path is invalid. Specify a valid file path."),
    BothFileAndFilePathProvided("%s0 Validation error. Both file and filePath are provided. Specify either file object or filePath, not both."),
    FileNotFoundToDeidentify("%s0 Validation error. The file to deidentify was not found at the specified path. Verify the file path and try again."),
    FileNotReadableToDeidentify("%s0 Validation error. The file to deidentify is not readable. Check the file permissions and try again."),
    InvalidPixelDensityToDeidentifyFile("%s0 Validation error. Should be a positive integer. Specify a valid pixel density."),
    InvalidMaxResolution("%s0 Validation error. Should be a positive integer. Specify a valid max resolution."),
    OutputDirectoryNotFound("%s0 Validation error. The output directory for deidentified files was not found at the specified path. Verify the output directory path and try again."),
    InvalidPermission("%s0 Validation error. The output directory for deidentified files is not writable. Check the directory permissions and try again."),
    InvalidWaitTime("%s0 Validation error. The wait time for deidentify file operation should be a positive integer. Specify a valid wait time."),
    WaitTimeExceedsLimit("%s0 Validation error. The wait time for deidentify file operation exceeds the maximum limit of 64 seconds. Specify a wait time less than or equal to 60 seconds."),
    InvalidOrEmptyRunId("%s0 Validation error. The run ID is invalid or empty. Specify a valid run ID."),
    FailedToEncodeFile("%s0 Validation error. Failed to encode the file. Ensure the file is in a supported format and try again."),
    FailedToDecodeFileFromResponse("%s0  Failed to decode the file from the response. Ensure the response is valid and try again."),
    EmptyFileAndFilePathInDeIdentifyFile("%s0 Validation error. Both file and filePath are empty. Specify either file object or filePath, not both."),
    PollingForResultsFailed("%s0 API error. Polling for results failed. Unable to retrieve the deidentified file"),
    FailedToSaveProcessedFile("%s0 Validation error. Failed to save the processed file. Ensure the output directory is valid and writable."),
    InvalidAudioFileType("%s0 Validation error. The file type is not supported. Specify a valid file type mp3 or wav."),
    // Generic
    ErrorOccurred("%s0 API error. Error occurred."),

    DetokenizeRequestNull("%s0 Validation error. DetokenizeRequest object is null. Specify a valid DetokenizeRequest object."),

    NullTokenGroupRedactions("%s0 Validation error. TokenGroupRedaction in the list is null. Specify a valid TokenGroupRedactions object."),

    NullRedactionInTokenGroup("%s0 Validation error. Redaction in TokenGroupRedactions is null or empty. Specify a valid redaction."),

    NullTokenGroupNameInTokenGroup("%s0 Validation error. TokenGroupName in TokenGroupRedactions is null or empty. Specify a valid tokenGroupName."),
    ;
    ;
    private final String message;

    ErrorMessage(String message) {
        this.message = message.replace("%s0", SdkVersion.getSdkPrefix());
    }

    public String getMessage() {
        return message;
    }
}
