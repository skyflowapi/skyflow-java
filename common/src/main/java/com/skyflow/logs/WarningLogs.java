package com.skyflow.logs;

public enum WarningLogs {
    OVERRIDING_EXISTING_VAULT_CONFIG("New vault config identified. Overriding existing vault config");

    private final String log;

    WarningLogs(String log) {
        this.log = log;
    }

    public final String getLog() {
        return log;
    }
}
