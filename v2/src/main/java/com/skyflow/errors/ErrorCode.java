/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.errors;

public enum ErrorCode {

    InvalidCredentialFilePath(400, "{Language} SDK v{semver} Initialization failed. Invalid credentials. Specify the file path as a string."),
    EmptyCredentialFilePath(400, "{Language} SDK v{semver} Initialization failed. Invalid credentials. Specify a valid file path."),
    InvalidRolesKeyType(4000, "{Language} SDK v{semver} Initialization failed. Invalid roles. Specify roles as an array."),
    InvalidRoleType(400, "{Language} SDK v{semver} Initialization failed. Invalid role. Specify the role as a string."),
    EmptyRoles(400, "{Language} SDK v{semver} Initialization failed. Invalid roles. Specify at least one role."),
    EmptyRoleInRoles(400, "{Language} SDK v{semver} Initialization failed. Invalid role. Specify a valid role."),
    InvalidContext(400, "{Language} SDK v{semver} Initialization failed. Invalid context provided. Specify context as type Context."),
    EmptyContext(400, "{Language} SDK v{semver} Initialization failed. Invalid context. Specify a valid context."),
    InvalidCredentialsString(400, "{Language} SDK v{semver} Initialization failed. Invalid credentials. Specify credentials as a string."),
    EmptyCredentialsString(400, "{Language} SDK v{semver} Initialization failed. Invalid credentials. Specify valid credentials."),
    EmptyVaultId(400, "{Language} SDK v{semver} Initialization failed. Invalid vault ID. Specify a valid vault ID."),
    InvalidVaultId(400, "{Language} SDK v{semver} Initialization failed. Invalid vault ID. Specify a valid vault ID as a string."),
    InvalidClusterId(400, "{Language} SDK v{semver} Initialization failed. Invalid cluster ID. Specify cluster ID as a string."),
    EmptyClusterId(400, "{Language} SDK v{semver} Initialization failed. Invalid cluster ID. Specify a valid cluster ID."),
    InvalidConnectionId(400, "{Language} SDK v{semver} Initialization failed. Invalid connection ID. Specify connection ID as a string."),
    EmptyConnectionId(400, "{Language} SDK v{semver} Initialization failed. Invalid connection ID. Specify a valid connection ID."),
    InvalidConnectionUrl(400, "{Language} SDK v{semver} Initialization failed. Invalid connection URL. Specify connection URL as a string."),
    EmptyConnectionUrl(400, "{Language} SDK v{semver} Initialization failed. Invalid connection URL. Specify a valid connection URL."),
    TokenProviderError(400, "{Language} SDK v{semver} Initialization failed. Invalid token provider. Specify the token provider as a function."),
    TokenProviderInvalidToken(400, "{Language} SDK v{semver} Initialization failed. Invalid token. Verify the output of the token provider."),
    FileNotFound(400, "{Language} SDK v{semver} Initialization failed. Credential file not found at %s1. Verify the file path."),
    FileInvalidJson(400, "{Language} SDK v{semver} Initialization failed. File at %s1 is not in valid JSON format. Verify the file contents."),
    InvalidCredentials(400, "{Language} SDK v{semver} Initialization failed. Invalid credentials provided. Specify valid credentials."),
    InvalidUrl(400, "{Language} SDK v{semver} Initialization failed. Vault URL %s1 is invalid. Specify a valid vault URL."),
    MissingPrivateKey(400, "{Language} SDK v{semver} Initialization failed. Unable to read private key in credentials. Verify your private key."),
    MissingClientId(400, "{Language} SDK v{semver} Initialization failed. Unable to read client ID in credentials. Verify your client ID."),
    MissingKeyId(400, "{Language} SDK v{semver} Initialization failed. Unable to read key ID in credentials. Verify your key ID."),
    MissingTokenUri(400, "{Language} SDK v{semver} Initialization failed. Unable to read token URI in credentials. Verify your token URI."),
    JwtInvalidFormat(400, "{Language} SDK v{semver} Initialization failed. Invalid private key format. Verify your credentials."),
    MissingAccessToken(400, "{Language} SDK v{semver} Validation error. Access token not present in the response from bearer token generation. Verify your credentials."),
    MissingTokenType(400, "{Language} SDK v{semver} Validation error. Token type not present in the response from bearer token generation. Verify your credentials."),
    JwtDecodeError(400, "{Language} SDK v{semver} Validation error. Invalid access token. Verify your credentials."),
    VaultIdNotInConfigList(400, "{Language} SDK v{semver} Validation error. VaultId is missing from the config. Specify the vaultIds from configs."),
    ConnectionIdNotInConfigList(400, "{Language} SDK v{semver} Validation error. ConnectionId is missing from the config. Specify the connectionIds from configs."),
    EmptyCredentials(400, "{Language} SDK v{semver} Validation error. Invalid credentials. Specify a valid credentials."),
    EmptyDataTokens(400, "{Language} SDK v{semver} Validation error. Invalid data tokens. Specify valid data tokens."),
    DataTokenKeyType(400, "{Language} SDK v{semver} Validation error. Invalid data tokens. Specify data token as an string array."),
    TimeToLeaveKeyType(400, "{Language} SDK v{semver} Validation error. Invalid time to live. Specify time to live parameter as an string."),

    //delete interface
    InvalidIdTypeDelete(400, "{Language} SDK v{semver} Validation error. Invalid 'id' in 'records' array at index %s1 in records array. Specify 'id' as a string."),
    EmptyRecordsInDelete(400, "{Language} SDK v{semver} Validation error. 'records' array can't be empty. Specify one or more records."),
    EmptyIDInDelete(400, "{Language} SDK v{semver} Validation error. 'id' can't be empty in the 'records' array. Specify an ID."),
    EmptyTableInDelete(400, "{Language} SDK v{semver} Validation error. 'table' can't be empty in the 'records' array. Specify a table."),

    //get interface
    InvalidIdsType(400, "{Language} SDK v{semver} Validation error. 'ids' has a value of type %s1. Specify 'ids' as list."),
    InvalidIdType(400, "{Language} SDK v{semver} Validation error. 'id' has a value of type %s1. Specify 'id' as string."),
    InvalidRedactionType(400, "{Language} SDK v{semver} Validation error. Redaction key has value of type %s1, expected Skyflow.Redaction."),
    InvalidColumnName(400, "{Language} SDK v{semver} Validation error. Column name has value of type %s1, expected a string."),
    InvalidColumnValue(400, "{Language} SDK v{semver} Validation error. Column values has value of type %s1, expected a list."),

    //connection
    InvalidPathParams(400, "{Language} SDK v{semver} Validation error. Path parameters aren't valid. Specify valid path parameters."),
    EmptyPathParams(400, "{Language} SDK v{semver} Validation error. Path parameters are empty. Specify valid path parameters."),
    InvalidRequestBody(400, "{Language} SDK v{semver} Validation error. Invalid request body. Specify the request body as an object."),
    EmptyRequestBody(400, "{Language} SDK v{semver} Validation error. Request body can't be empty. Specify a valid request body."),

    //insert
    RecordsKeyError(400, "{Language} SDK v{semver} Validation error. 'records' key is missing from the payload. Specify a 'records' key."),
    FieldsKeyError(400, "{Language} SDK v{semver} Validation error. 'fields' key is missing from the payload. Specify a 'fields' key."),
    TableKeyError(400, "{Language} SDK v{semver} Validation error. 'table' key is missing from the payload. Specify a 'table' key."),
    TokenKeyError(400, "{Language} SDK v{semver} Validation error. 'token' key is missing from the payload. Specify a 'token' key."),
    IdsKeyError(400, "{Language} SDK v{semver} Validation error. 'ids' key is missing from the payload. Specify an 'ids' key."),
    RedactionKeyError(400, "{Language} SDK v{semver} Validation error. 'redaction' key is missing from the payload. Specify a 'redaction' key."),
    UniqueColumnOrIdsKeyError(400, "{Language} SDK v{semver} Validation error. ids or columnName key is missing from the payload. Specify the ids or unique 'columnName' in payload."),
    UpdateFieldKeyError(400, "{Language} SDK v{semver} Validation error. Fields are empty in an update payload. Specify at least one field."),
    InvalidRecordsType(400, "{Language} SDK v{semver} Validation error. The 'records' key has a value of type %s1. Specify 'records' as a list."),
    InvalidFieldsType(400, "{Language} SDK v{semver} Validation error. The 'fields' key has a value of type %s1. Specify 'fields' as a dictionary."),
    InvalidTokensType(400, "{Language} SDK v{semver} Validation error. The 'tokens' key has a value of type %s1. Specify 'tokens' as a dictionary."),
    EmptyTokensInInsert(400, "{Language} SDK v{semver} Validation error. The 'tokens' field is empty. Specify tokens for one or more fields."),
    MismatchOfFieldsAndTokens(400, "{Language} SDK v{semver} Validation error. 'fields' and 'tokens' have different columns names. Verify that 'fields' and 'tokens' columns match."),
    InvalidTableType(400, "{Language} SDK v{semver} Validation error. The 'table' key has a value of type %s1. Specify 'table' as a string."),
    RedactionWithTokensNotSupported(400, "{Language} SDK v{semver} Validation error. 'redaction' can't be used when tokens are specified. Remove 'redaction' from payload if tokens are specified."),
    TokensGetColumnNotSupported(400, "{Language} SDK v{semver} Validation error. Column name and/or column values can't be used when tokens are specified. Remove unique column values or tokens from the payload."),
    BothIdsAndColumnDetailsSpecified(400, "{Language} SDK v{semver} Validation error. Both Skyflow IDs and column details can't be specified. Either specify Skyflow IDs or unique column details."),
    PartialSuccess(400, "{Language} SDK v{semver} Validation error. Check 'SkyflowError.data' for details."),
    InvalidUpsertOptionsType(400, "{Language} SDK v{semver} Validation error. 'upsert' key cannot be empty in options. At least one object of table and column is required."),
    InvalidUpsertTableType(400, "{Language} SDK v{semver} Validation error. The 'table' key in the 'upsert' object has avalue of type %s1. Specify 'table' as a string."),
    InvalidUpsertColumnType(400, "{Language} SDK v{semver} Validation error. The 'column' key in the 'upsert' object has a value of type %s1. Specify 'column' as a string."),
    EmptyUpsertOptionTable(400, "{Language} SDK v{semver} Validation error. The 'table' key in the 'upsert' object can't be an empty string. Specify a 'table' value."),
    EmptyUpsertOptionColumn(400, "{Language} SDK v{semver} Validation error. The 'column' key in the 'upsert' object can't be an empty string. Specify a 'column' value."),
    BatchInsertPartialSuccess(400, "{Language} SDK v{semver} Insert operation completed with partial success."),
    BatchInsertFailure(400, "{Language} SDK v{semver} Insert operation failed."),
    InvalidByotType(400, "{Language} SDK v{semver} Validation error. The 'byot' key has a value of type %s1. Specify 'byot' as Skyflow.BYOT."),
    NoTokensInInsert(400, "{Language} SDK v{semver} Validation error. Tokens weren't specified for records while 'byot' was %s1. Specify tokens."),
    TokensPassedForByotDisable(400, "{Language} SDK v{semver} Validation error. 'byot' wasn't specified. Set 'byot' to 'ENABLE' to insert tokens."),
    InsufficientTokensPassedForByotEnableStrict(400, "{Language} SDK v{semver} Validation error. 'byot' is set to 'ENABLE_STRICT', but some fields are missing tokens. Specify tokens for all fields.");



    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }

}
