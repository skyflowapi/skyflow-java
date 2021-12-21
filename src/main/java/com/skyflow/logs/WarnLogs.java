package com.skyflow.logs;

public enum WarnLogs {
    GetTokenDeprecated("GetToken method is Deprecated, will be removed in future, use GenerateBearerToken")
    ;
    private final String log;

    WarnLogs(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}
