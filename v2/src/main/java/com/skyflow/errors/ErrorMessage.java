package com.skyflow.errors;

import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;

public enum ErrorMessage {
    // client initialization
    VaultIdAlreadyInConfigList("%s1 Validation error. VaultId is present in an existing config. Specify a new vaultId in config."),
    VaultIdNotInConfigList("%s1 Validation error. VaultId is missing from the config. Specify the vaultIds from configs."),
    ConnectionIdAlreadyInConfigList("%s1 Validation error. ConnectionId is present in an existing config. Specify a connectionId in config."),
    ConnectionIdNotInConfigList("%s1 Validation error. ConnectionId is missing from the config. Specify the connectionIds from configs."),
    EmptyCredentials("%s1 Validation error. Invalid credentials. Specify a valid credentials."),

    // vault config
    InvalidVaultId("%s1 Initialization failed. Invalid vault ID. Specify a valid vault ID."),
    EmptyVaultId("%s1 Initialization failed. Invalid vault ID. Vault ID must not be empty."),
    InvalidClusterId("%s1 Initialization failed. Invalid cluster ID. Specify cluster ID."),
    EmptyClusterId("%s1 Initialization failed. Invalid cluster ID. Specify a valid cluster ID."),

    // connection config
    InvalidConnectionId("%s1 Initialization failed. Invalid connection ID. Specify a valid connection ID."),
    EmptyConnectionId("%s1 Initialization failed. Invalid connection ID. Connection ID must not be empty."),
    EmptyConnectionUrl("%s1 Initialization failed. Invalid connection URL. Connection URL must not be empty."),
    InvalidConnectionUrl("%s1 Initialization failed. Invalid connection URL. Specify a valid connection URL."),
    InvalidConnectionUrlFormat("%s1 Initialization failed. Connection URL is not a valid URL. Specify a valid connection URL."),

    // credentials
    MultipleTokenGenerationMeansPassed("%s1 Initialization failed. Invalid credentials. Specify only one from 'path', 'credentialsString', 'token' or 'apiKey'."),
    NoTokenGenerationMeansPassed("%s1 Initialization failed. Invalid credentials. Specify any one from 'path', 'credentialsString', 'token' or 'apiKey'."),
    EmptyCredentialFilePath("%s1 Initialization failed. Invalid credentials. Credentials file path must not be empty."),
    EmptyCredentialsString("%s1 Initialization failed. Invalid credentials. Credentials string must not be empty."),
    EmptyToken("%s1 Initialization failed. Invalid credentials. Token mut not be empty."),
    EmptyApikey("%s1 Initialization failed. Invalid credentials. Api key must not be empty."),
    InvalidApikey("%s1 Initialization failed. Invalid credentials. Specify valid api key."),
    EmptyRoles("%s1 Initialization failed. Invalid roles. Specify at least one role."),
    EmptyRoleInRoles("%s1 Initialization failed. Invalid role. Specify a valid role."),
    EmptyContext("%s1 Initialization failed. Invalid context. Specify a valid context."),

    // bearer token generation
    FileNotFound("%s1 Initialization failed. Credential file not found at %s2. Verify the file path."),
    FileInvalidJson("%s1 Initialization failed. File at %s2 is not in valid JSON format. Verify the file contents."),
    CredentialsStringInvalidJson("%s1 Initialization failed. Credentials string is not in valid JSON format. Verify the credentials string contents."),
    InvalidCredentials("%s1 Initialization failed. Invalid credentials provided. Specify valid credentials."),
    MissingPrivateKey("%s1 Initialization failed. Unable to read private key in credentials. Verify your private key."),
    MissingClientId("%s1 Initialization failed. Unable to read client ID in credentials. Verify your client ID."),
    MissingKeyId("%s1 Initialization failed. Unable to read key ID in credentials. Verify your key ID."),
    MissingTokenUri("%s1 Initialization failed. Unable to read token URI in credentials. Verify your token URI."),
    InvalidTokenUri("%s1 Initialization failed. Token URI in not a valid URL in credentials. Verify your token URI."),
    JwtInvalidFormat("%s1 Initialization failed. Invalid private key format. Verify your credentials."),
    InvalidAlgorithm("%s1 Initialization failed. Invalid algorithm to parse private key. Specify valid algorithm."),
    InvalidKeySpec("%s1 Initialization failed. Unable to parse RSA private key. Verify your credentials."),
    JwtDecodeError("%s1 Validation error. Invalid access token. Verify your credentials."),
    MissingAccessToken("%s1 Validation error. Access token not present in the response from bearer token generation. Verify your credentials."),
    MissingTokenType("%s1 Validation error. Token type not present in the response from bearer token generation. Verify your credentials."),

    // insert
    TableKeyError("%s1 Validation error. 'table' key is missing from the payload. Specify a 'table' key."),
    EmptyTable("%s1 Validation error. 'table' can't be empty. Specify a table."),
    ValuesKeyError("%s1 Validation error. 'values' key is missing from the payload. Specify a 'values' key."),
    EmptyValues("%s1 Validation error. 'values' can't be empty. Specify values."),
    EmptyKeyInValues("%s1 Validation error. Invalid key in values. Specify a valid key."),
    EmptyValueInValues("%s1 Validation error. Invalid value in values. Specify a valid value."),
    TokensKeyError("%s1 Validation error. 'tokens' key is missing from the payload. Specify a 'tokens' key."),
    EmptyTokens("%s1 Validation error. The 'tokens' field is empty. Specify tokens for one or more fields."),
    EmptyKeyInTokens("%s1 Validation error. Invalid key tokens. Specify a valid key."),
    EmptyValueInTokens("%s1 Validation error. Invalid value in tokens. Specify a valid value."),
    EmptyUpsert("%s1 Validation error. 'upsert' key can't be empty. Specify an upsert column."),
    HomogenousNotSupportedWithUpsert("%s1 Validation error. 'homogenous' is not supported with 'upsert'. Specify either 'homogenous' or 'upsert'."),
    TokensPassedForByotDisable("%s1 Validation error. 'tokenStrict' wasn't specified. Set 'tokenStrict' to 'ENABLE' to insert tokens."),
    NoTokensWithByot("%s1 Validation error. Tokens weren't specified for records while 'tokenStrict' was %s2. Specify tokens."),
    MismatchOfFieldsAndTokens("%s1 Validation error. 'fields' and 'tokens' have different columns names. Verify that 'fields' and 'tokens' columns match."),
    InsufficientTokensPassedForByotEnableStrict("%s1 Validation error. 'tokenStrict' is set to 'ENABLE_STRICT', but some fields are missing tokens. Specify tokens for all fields."),
    BatchInsertPartialSuccess("%s1 Insert operation completed with partial success."),
    BatchInsertFailure("%s1 Insert operation failed."),

    // detokenize
    InvalidDataTokens("%s1 Validation error. Invalid data tokens. Specify valid data tokens."),
    EmptyDataTokens("%s1 Validation error. Invalid data tokens. Specify at least one data token."),
    EmptyTokenInDataTokens("%s1 Validation error. Invalid data tokens. Specify a valid data token."),

    // get interface
    IdsKeyError("%s1 Validation error. 'ids' key is missing from the payload. Specify an 'ids' key."),
    EmptyIds("%s1 Validation error. 'ids' can't be empty. Specify at least one id."),
    EmptyIdInIds("%s1 Validation error. Invalid id in 'ids'. Specify a valid id."),
    EmptyFields("%s1 Validation error. Fields are empty in get payload. Specify at least one field."),
    EmptyFieldInFields("%s1 Validation error. Invalid field in 'fields'. Specify a valid field."),
    RedactionKeyError("%s1 Validation error. 'redaction' key is missing from the payload. Specify a 'redaction' key."),
    RedactionWithTokensNotSupported("%s1 Validation error. 'redaction' can't be used when 'returnTokens' is specified. Remove 'redaction' from payload if 'returnTokens' is specified."),
    TokensGetColumnNotSupported("%s1 Validation error. Column name and/or column values can't be used when 'returnTokens' is specified. Remove unique column values or 'returnTokens' from the payload."),
    EmptyOffset("%s1 Validation error. 'offset' can't be empty. Specify an offset."),
    EmptyLimit("%s1 Validation error. 'limit' can't be empty. Specify a limit."),
    UniqueColumnOrIdsKeyError("%s1 Validation error. 'ids' or 'columnName' key is missing from the payload. Specify the ids or unique 'columnName' in payload."),
    BothIdsAndColumnDetailsSpecified("%s1 Validation error. Both Skyflow IDs and column details can't be specified. Either specify Skyflow IDs or unique column details."),
    ColumnNameKeyError("%s1 Validation error. 'columnName' isn't specified whereas 'columnValues' are specified. Either add 'columnName' or remove 'columnValues'."),
    EmptyColumnName("%s1 Validation error. 'columnName' can't be empty. Specify a column name."),
    ColumnValuesKeyErrorGet("%s1 Validation error. 'columnValues' aren't specified whereas 'columnName' is specified. Either add 'columnValues' or remove 'columnName'."),
    EmptyColumnValues("%s1 Validation error. 'columnValues' can't be empty. Specify at least one column value"),
    EmptyValueInColumnValues("%s1 Validation error. Invalid value in column values. Specify a valid column value."),

    TokenKeyError("%s1 Validation error. 'token' key is missing from the payload. Specify a 'token' key."),
    PartialSuccess("%s1 Validation error. Check 'SkyflowError.data' for details."),

    // update
    SkyflowIdKeyError("%s1 Validation error. 'id' key is missing from the payload. Specify an 'id' key."),
    EmptySkyflowId("%s1 Validation error. 'id' can't be empty. Specify an id."),

    // query
    QueryKeyError("%s1 Validation error. 'query' key is missing from the payload. Specify a 'query' key."),
    EmptyQuery("%s1 Validation error. 'query' can't be empty. Specify a query"),

    // tokenize
    ColumnValuesKeyErrorTokenize("%s1 Validation error. 'columnValues' key is missing from the payload. Specify a 'columnValues' key."),
    EmptyColumnGroupInColumnValue("%s1 Validation error. Invalid column group in column value. Specify a valid column group."),

    //connection
    InvalidPathParams("%s1 Validation error. Path parameters aren't valid. Specify valid path parameters."),
    EmptyPathParams("%s1 Validation error. Path parameters are empty. Specify valid path parameters."),
    InvalidRequestBody("%s1 Validation error. Invalid request body. Specify the request body as an object."),
    EmptyRequestBody("%s1 Validation error. Request body can't be empty. Specify a valid request body."),

    ;
    private final String message;

    ErrorMessage(String message) {
        this.message = Utils.parameterizedString(message, Constants.SDK_PREFIX);
    }

    public String getMessage() {
        return message;
    }
}
