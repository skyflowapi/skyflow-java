/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.LogLevel;
import com.skyflow.logs.InfoLogs;

/**
 * This is the description for Configuration Class.
 */
public final class Configuration {
    /**
     * This is the description for setLogLevel method.
     * @param level This is the description of level paramter. 
     */
    public static void setLogLevel(LogLevel level) {
        LogUtil.setupLogger(level);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.CurrentLogLevel.getLog(), String.valueOf(level)));
    }
}
