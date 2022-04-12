package com.skyflow.logs;

public enum DebugLogs {

    FormatRequestBodyFormUrlFormEncoded("Formatting request body for form-urlencoded content-type"),
    FormatRequestBodyFormData("Formatting request body for form-data content-type");
    private final String log;

    DebugLogs(String log) {
        this.log = log;
    }

    public String getLog() {
        return log;
    }
}