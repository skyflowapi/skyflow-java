package com.skyflow;

import com.skyflow.common.utils.Helpers;
import com.skyflow.common.utils.LogUtil;
import com.skyflow.entities.LogLevel;
import com.skyflow.logs.InfoLogs;

public final class Configuration {
    public static void setLogLevel(LogLevel level) {
        LogUtil.setupLogger(level);
        LogUtil.printInfoLog(Helpers.parameterizedString(InfoLogs.CurrentLogLevel.getLog(), String.valueOf(level)));
    }
}
