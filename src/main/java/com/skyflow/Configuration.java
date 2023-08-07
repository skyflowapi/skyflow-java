/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.LogLevel;
import com.skyflow.logs.InfoLogs;

/**
 * Configuration for the skyflow client.
 */
public final class Configuration {
    /**
     * Sets log level for the client.
     * @param level Required log level.
     */
    public static void setLogLevel(LogLevel level) {
        LogUtil.setupLogger(level);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.CurrentLogLevel.getLog(), String.valueOf(level)));
    }
}
