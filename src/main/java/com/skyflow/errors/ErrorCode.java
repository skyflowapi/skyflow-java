package com.skyflow.errors;

public enum ErrorCode {
    InvalidVaultURL(400, "Invalid Vault URL"),
    EmptyVaultID(400,"Empty Vault ID"),
    InvalidKeySpec(400,"Unable to parse RSA private key"),
    NoSuchAlgorithm(400,"Invalid algorithm"),
    UnableToRetrieveRSA(400,"Unable to retrieve RSA private key"),
    UnableToReadResponse(400,"Unable to read response payload"),
    InvalidTokenURI(400,"Unable to read tokenURI"),
    InvalidKeyID(400,"Unable to read keyID"),
    InvalidClientID(400,"Unable to read clientID"),
    InvalidCredentialsPath(400,"Unable to open credentials - file %s1"),
    InvalidJsonFormat(400,"Provided json file is in wrong format - file %s1"),
    InvalidBearerToken(400,"Invalid token"),
    BearerThrownException(400,"getBearer() thrown exception"),
    InvalidDetokenizeInput(400,"Invalid Detokenize Input"),
    InvalidInsertInput(400,"Invalid insert input"),
    InvalidGetByIdInput(400,"Invalid getById input"),
    ResponseParsingError(500,"Unable to parse response"),
    ThreadInterruptedException(500, "Thread was interrupted"),
    ThreadExecutionException(500, "ThreadExecution exception"),
    Server(500, "Internal server error")
    ;
    private final int code;
    private final String description;
    ErrorCode(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
