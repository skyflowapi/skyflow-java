package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.enums.LogLevel;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

class BaseSkyflow {
    private final BaseSkyflowClientBuilder builder;

    protected BaseSkyflow(BaseSkyflowClientBuilder builder) {
        this.builder = builder;
    }

    public LogLevel getLogLevel() {
        return this.builder.logLevel;
    }

//    public VaultConfig getVaultConfig() {
//        Object[] array = this.builder.vaultConfigMap.values().toArray();
//        return (VaultConfig) array[0];
//    }

    static class BaseSkyflowClientBuilder {
        //        protected final LinkedHashMap<String, VaultConfig> vaultConfigMap;
        protected Credentials skyflowCredentials;
        protected LogLevel logLevel;

        protected BaseSkyflowClientBuilder() {
//            this.vaultConfigMap = new LinkedHashMap<>();
            this.skyflowCredentials = null;
            this.logLevel = LogLevel.ERROR;
        }

        public BaseSkyflowClientBuilder setLogLevel(LogLevel logLevel) {
            this.logLevel = logLevel == null ? LogLevel.ERROR : logLevel;
            LogUtil.setupLogger(this.logLevel);
            LogUtil.printInfoLog(BaseUtils.parameterizedString(
                    InfoLogs.CURRENT_LOG_LEVEL.getLog(), String.valueOf(logLevel)
            ));
            return this;
        }
    }
}
