package com.skyflow.entities;

public class Options {
    private final LogLevel logLevel;

    public Options(LogLevel logLevel) {
        this.logLevel = logLevel;
    }


    public LogLevel getLogLevel() {
        return logLevel;
    }


}
