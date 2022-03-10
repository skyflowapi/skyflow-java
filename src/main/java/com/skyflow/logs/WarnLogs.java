package com.skyflow.logs;

public enum WarnLogs {
    GetTokenDeprecated("GenerateToken method is deprecated, will be removed in future, use generateBearerToken()"),
    IsValidDeprecated("IsValid method is deprecated, will be removed in future, use isExpired()");
    private final String log;

    WarnLogs(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
