/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.logs;

public enum ErrorLogs {
    InvalidVaultId("empty or invalid vaultID"),
    InvalidVaultURL("invalid vault url"),
    InvalidTokenProvider("invalid TokenProvider. TokenProvider cannot be null"),
    InvalidInsertInput("invalid insert input"),
    InvalidDetokenizeInput("invalid detokenize input"),
    ResponseParsingError("Unable to parse response in %s1 method"),
    ThreadInterruptedException("Thread was interrupted in %s1 method"),
    ThreadExecutionException("ThreadExecution exception in %s1 method"),
    InvalidGetByIdInput("Invalid getById input"),
    InvalidInvokeConnectionInput("Invalid invokeConnection Input"),
    ConnectionURLMissing("connectionURL is required"),
    InvalidConnectionURL("Invalid connectionURL"),
    MethodNameMissing("methodName is required"),
    InvalidMethodName("methodName is invalid"),
    InvalidKeySpec("Unable to parse RSA private key"),
    NoSuchAlgorithm("Invalid algorithm"),
    UnableToRetrieveRSA("Unable to retrieve RSA private key"),
    UnableToReadResponse("Unable to read response payload"),
    InvalidTokenURI("Unable to read tokenURI"),
    InvalidKeyID("Unable to read keyID"),
    InvalidClientID("Unable to read clientID"),
    InvalidCredentialsPath("Unable to open credentials - file %s1"),
    InvalidJsonFormat("Provided json file is in wrong format - file %s1"),
    EmptyJSONString("credentials string cannot be empty or null"),
    EmptyFilePath("file path cannot be empty or null"),
    InvalidJSONStringFormat("credentials string is not a valid json string format"),
    BearerThrownException("getBearer() thrown exception "),
    InvalidBearerToken("Invalid Bearer token"),
    InvalidTable("Table name is missing"),
    Server("Internal server error"),
    InvalidUpsertOptionType("upsert options cannot be null, should be an non empty UpsertOption array."),
    InvalidTableInUpsertOption("Invalid table in upsert object, non empty string is required."),
    InvalidColumnInUpsertOption("Invalid column in upsert object, non empty string is required.");
    private final String log;

    ErrorLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
