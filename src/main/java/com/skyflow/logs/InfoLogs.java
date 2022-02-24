package com.skyflow.logs;

public enum InfoLogs {
    EmptyBearerToken("BearerToken is empty"),
    InitializedClient("skyflow client initialized successfully"),
    CurrentLogLevel("client LogLevel is %s1"),
    LoggerSetup("logger has setup successfully"),
    ValidatingSkyflowConfiguration("validating skyflow configuration"),
    ValidatedSkyflowConfiguration("validated skyflow configuration in %s1 method"),
    ValidatingInvokeConnectionConfig("validating invoke connection configuration"),
    InsertMethodCalled("insert method has triggered"),
    DetokenizeMethodCalled("detokenize method has triggered"),
    GetByIdMethodCalled("getById method has triggered"),
    InvokeConnectionCalled("invokeConnection method has triggered"),
    GenerateBearerTokenCalled("generateBearerToken method has triggered"),
    GenerateBearerTokenFromCredsCalled("generateBearerTokenFromCreds method has triggered"),
    ;
    private final String log;

    InfoLogs(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
