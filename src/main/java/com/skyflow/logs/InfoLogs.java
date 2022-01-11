package com.skyflow.logs;

public enum InfoLogs {
    InitializedClient("skyflow client initialized successfully"),
    CurrentLogLevel("client LogLevel is %s1"),
    LoggerSetup("logger setup successfully"),
    ValidatingSkyflowConfiguration("validating skyflow configuration"),
    ValidatedSkyflowConfiguration("validated skyflow configuration in %s1 method"),
    ValidatingInvokeConnectionConfig("validating invoke connection configuration"),
    InsertMethodCalled("client called insert method"),
    DetokenizeMethodCalled("client called detokenize method"),
    GetByIdMethodCalled("client called getById method"),
    InvokeConnectionCalled("client called invoke connection method"),
    GenerateBearerTokenCalled("client called generateBearerToken method"),
    GenerateBearerTokenFromCredsCalled("client called generateBearerTokenFromCreds method"),
    ;
    private final String log;

    InfoLogs(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
