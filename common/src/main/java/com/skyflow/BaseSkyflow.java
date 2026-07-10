package com.skyflow;

import com.skyflow.enums.LogLevel;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

class BaseSkyflow {
    private final BaseSkyflowClientBuilder builder;

    protected BaseSkyflow(BaseSkyflowClientBuilder builder) {
        this.builder = builder;
        LogUtil.printInfoLog(InfoLogs.CLIENT_INITIALIZED.getLog());
    }

    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

    static class BaseSkyflowClientBuilder {
        protected LogLevel logLevel = LogLevel.ERROR;

        protected BaseSkyflowClientBuilder() {
        }

        protected BaseSkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(BaseUtils.parameterizedString(
                    InfoLogs.CURRENT_LOG_LEVEL.getLog(), String.valueOf(this.logLevel)
            ));
            return this;
        }
    }

}
