/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.entities.LogLevel;
import com.skyflow.logs.InfoLogs;

import java.util.logging.*;

public final class LogUtil {
    private static final Logger LOGGER = Logger.getLogger(LogUtil.class.getName());
    private static final String SDK_OWNER = "[Skyflow] ";
    private static boolean IS_LOGGER_SETUP_DONE = false;


    synchronized public static void setupLogger(LogLevel logLevel) {
        IS_LOGGER_SETUP_DONE = true;
        LogManager.getLogManager().reset();
        LOGGER.setUseParentHandlers(false);
        Formatter formatter = new SimpleFormatter() {
            private static final String format = "%s: %s %n";

            // Override format method
            @Override
            public synchronized String format(LogRecord logRecord) {
                return String.format(format, loggerLevelToLogLevelMap(logRecord.getLevel()),
                        logRecord.getMessage());
            }
        };
        ConsoleHandler consoleHandler = new ConsoleHandler();
        consoleHandler.setFormatter(formatter);
        consoleHandler.setLevel(Level.CONFIG);

        LOGGER.addHandler(consoleHandler);
        LOGGER.setLevel(logLevelToLoggerLevelMap(logLevel));
        printInfoLog(InfoLogs.LoggerSetup.getLog());
    }

    public static void printErrorLog(String message) {
        if (IS_LOGGER_SETUP_DONE)
            LOGGER.severe(SDK_OWNER + message);
        else {
            setupLogger(LogLevel.ERROR);
            LOGGER.severe(SDK_OWNER + message);
        }
    }

    public static void printDebugLog(String message) {
        if (IS_LOGGER_SETUP_DONE)
            LOGGER.config(SDK_OWNER + message);
    }

    public static void printWarningLog(String message) {
        if (IS_LOGGER_SETUP_DONE)
            LOGGER.warning(SDK_OWNER + message);
    }

    public static void printInfoLog(String message) {
        if (IS_LOGGER_SETUP_DONE)
            LOGGER.info(SDK_OWNER + message);
    }


    private static Level logLevelToLoggerLevelMap(LogLevel logLevel) {
        Level loggerLevel;
        switch (logLevel) {
            case ERROR:
                loggerLevel = Level.SEVERE;
                break;
            case WARN:
                loggerLevel = Level.WARNING;
                break;
            case INFO:
                loggerLevel = Level.INFO;
                break;
            case DEBUG:
                loggerLevel = Level.CONFIG;
                break;
            default:
                loggerLevel = Level.OFF;
        }
        return loggerLevel;
    }

    private static LogLevel loggerLevelToLogLevelMap(Level loggerLevel) {
        LogLevel logLevel;
        if (Level.SEVERE.equals(loggerLevel)) {
            logLevel = LogLevel.ERROR;
        } else if (Level.WARNING.equals(loggerLevel)) {
            logLevel = LogLevel.WARN;
        } else if (Level.INFO.equals(loggerLevel)) {
            logLevel = LogLevel.INFO;
        } else if (Level.CONFIG.equals(loggerLevel)){
            logLevel = LogLevel.DEBUG;
        }else
            logLevel = LogLevel.OFF;
        return logLevel;
    }


}
