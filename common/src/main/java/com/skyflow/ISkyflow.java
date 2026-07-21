package com.skyflow;

import com.skyflow.config.BaseCredentials;
import com.skyflow.config.BaseVaultConfig;
import com.skyflow.enums.LogLevel;
import com.skyflow.errors.SkyflowException;

public interface ISkyflow<Self, V extends BaseVaultConfig, C extends BaseCredentials> {
    Self addVaultConfig(V vaultConfig) throws SkyflowException;

    Self updateVaultConfig(V vaultConfig) throws SkyflowException;

    Self removeVaultConfig(String vaultId) throws SkyflowException;

    Self updateSkyflowCredentials(C credentials) throws SkyflowException;

    Self setLogLevel(LogLevel logLevel);

    LogLevel getLogLevel();
}
