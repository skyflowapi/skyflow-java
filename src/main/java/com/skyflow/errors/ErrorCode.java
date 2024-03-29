/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.errors;

public enum ErrorCode {
    InvalidVaultURL(400, "Invalid Vault URL"),
    EmptyVaultID(400, "Empty Vault ID"),
    InvalidTokenProvider(400, "Invalid TokenProvider"),
    InvalidKeySpec(400, "Unable to parse RSA private key"),
    NoSuchAlgorithm(400, "Invalid algorithm"),
    UnableToRetrieveRSA(400, "Unable to retrieve RSA private key"),
    UnableToReadResponse(400, "Unable to read response payload"),
    InvalidTokenURI(400, "Unable to read tokenURI"),
    InvalidKeyID(400, "Unable to read keyID"),
    InvalidClientID(400, "Unable to read clientID"),
    InvalidCredentialsPath(400, "Unable to open credentials - file %s1"),
    InvalidJsonFormat(400, "Provided json file is in wrong format - file %s1"),
    InvalidBearerToken(400, "Invalid token"),
    BearerThrownException(400, "getBearer() thrown exception"),
    EmptyRecords(400, "Records cannot be empty"),
    InvalidTable(400, "Table name is missing"),
    InvalidId(400, "Skyflow id is missing"),
    InvalidFields(400, "Fields are missing"),
    InvalidQueryInput(400, "Invalid query input"),
    InvalidQuery(400, "Query is missing"),
    InvalidSkyflowId(400, "Skyflow id are missing"),
    InvalidToken(400, "Token is empty"),
    InvalidDetokenizeInput(400, "Invalid Detokenize Input"),
    InvalidInsertInput(400, "Invalid insert input"),
    InvalidUpdateInput(400, "Invalid update input"),
    InvalidDeleteInput(400, "Invalid delete input"),
    InvalidGetByIdInput(400, "Invalid getById input"),
    InvalidGetInput(400, "Invalid get input"),
    MissingIdAndColumnName(400, "Provide either Ids or column name to get records."),
    SkyflowIdAndColumnNameBothSpecified(400, "ids and columnName can not be specified together."),
    MissingRecordColumnValue(400, "Column Values can not be empty when Column Name is specified."),
    MissingRecordColumnName(400, "Column Name can not be empty when Column Values are specified."),
    ResponseParsingError(500, "Unable to parse response"),
    ThreadInterruptedException(500, "Thread was interrupted"),
    ThreadExecutionException(500, "ThreadExecution exception"),
    Server(500, "Internal server error"),
    ServerReturnedErrors(500, "Server returned errors, check SkyflowException.getData() for more"),
    ConnectionURLMissing(400, "connectionURL is required"),
    InvalidConnectionURL(400, "Invalid connectionURL"),
    MethodNameMissing(400, "methodName is required"),
    InvalidMethodName(400, "methodName is invalid"),
    InvalidConnectionInput(400, "Invalid connection Input"),
    EmptyJSONString(400, "credentials string cannot be empty"),
    InvalidJSONStringFormat(400, "credentials string is not a valid json string format"),
    EmptyFilePath(400, "file path cannot be empty or null"),
    EmptyContext(400, "ctx claim field is missing from the jwt assertion"),
    IncorrectRole(400, "Requested scope  cannot be granted"),
    IncorrectCredentials(400, "Incorrect credentials provided"),
    InvalidUpsertOptionType(400, "upsert options should be an non empty UpsertOption array."),
    InvalidUpsertObjectType(400, "upsert option cannot be null, should be an UpsertOption object."),
    InvalidTableInUpsertOption(400, "Invalid table in upsert object, non empty string is required."),
    InvalidColumnInUpsertOption(400, "Invalid column in upsert object, non empty string is required."),
    MissingRedaction(400, "Missing Redaction Property"),
    TokensGetColumnNotSupported(400,"Interface: get method - column_name or column_values cannot be used with tokens in options."),
    RedactionWithTokenNotSupported(400, "Interface: get method - redaction cannot be used when tokens are true in options."),

    BearerTokenExpired(400,"Bearer token is invalid or expired.");
    private final int code;
    private final String description;

    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String  getDescription() {
        return description;
    }
}
