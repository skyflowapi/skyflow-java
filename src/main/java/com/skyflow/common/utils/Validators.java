/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.entities.*;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import org.json.simple.JSONObject;

import java.net.URL;
import java.util.Objects;

public final class Validators {
    public static void validateConfiguration(SkyflowConfiguration config) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.ValidatingSkyflowConfiguration.getLog());
        if (config.getVaultID() == null || config.getVaultID().length() <= 0) {
            LogUtil.printErrorLog(ErrorLogs.InvalidVaultId.getLog());
            throw new SkyflowException(ErrorCode.EmptyVaultID);
        }

        if (config.getVaultURL() == null || isInvalidURL(config.getVaultURL())) {
            LogUtil.printErrorLog(ErrorLogs.InvalidVaultURL.getLog());
            throw new SkyflowException(ErrorCode.InvalidVaultURL);
        }

        if (config.getTokenProvider() == null) {
            LogUtil.printErrorLog(ErrorLogs.InvalidTokenProvider.getLog());
            throw new SkyflowException(ErrorCode.InvalidTokenProvider);
        }

    }

    public static void validateConnectionConfiguration(JSONObject connectionConfig, SkyflowConfiguration skyflowConfiguration) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.ValidatingInvokeConnectionConfig.getLog());
        if (skyflowConfiguration.getTokenProvider() == null) {
            LogUtil.printErrorLog(ErrorLogs.InvalidTokenProvider.getLog());
            throw new SkyflowException(ErrorCode.InvalidTokenProvider);
        }

        if (connectionConfig.containsKey("connectionURL")) {
            String connectionURL = (String) connectionConfig.get("connectionURL");
            if (isInvalidURL(connectionURL)) {
                LogUtil.printErrorLog(ErrorLogs.InvalidConnectionURL.getLog());
                throw new SkyflowException(ErrorCode.InvalidConnectionURL);
            }
        } else {
            LogUtil.printErrorLog(ErrorLogs.ConnectionURLMissing.getLog());
            throw new SkyflowException(ErrorCode.ConnectionURLMissing);
        }

        if (connectionConfig.containsKey("methodName")) {
            try {
                RequestMethod requestMethod = (RequestMethod) connectionConfig.get("methodName");
            } catch (Exception e) {
                LogUtil.printErrorLog(ErrorLogs.InvalidMethodName.getLog());
                throw new SkyflowException(ErrorCode.InvalidMethodName);
            }
        } else {
            LogUtil.printErrorLog(ErrorLogs.MethodNameMissing.getLog());
            throw new SkyflowException(ErrorCode.MethodNameMissing);
        }
    }

    public static void validateUpsertOptions(UpsertOption[] upsertOptions) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.ValidatingUpsertOptions.getLog());
        if (upsertOptions.length == 0) {
            LogUtil.printErrorLog(ErrorLogs.InvalidUpsertOptionType.getLog());
            throw new SkyflowException(ErrorCode.InvalidUpsertOptionType);
        }

        for (UpsertOption upsertOption : upsertOptions) {
            if (upsertOption == null) {
                LogUtil.printErrorLog(ErrorLogs.InvalidUpsertObjectType.getLog());
                throw new SkyflowException(ErrorCode.InvalidUpsertObjectType);
            }

            if (upsertOption.getTable() == null || Objects.equals(upsertOption.getTable(), "")) {
                LogUtil.printErrorLog(ErrorLogs.InvalidTableInUpsertOption.getLog());
                throw new SkyflowException(ErrorCode.InvalidTableInUpsertOption);
            }
            if (upsertOption.getColumn() == null || Objects.equals(upsertOption.getColumn(), "")) {
                LogUtil.printErrorLog(ErrorLogs.InvalidColumnInUpsertOption.getLog());
                throw new SkyflowException(ErrorCode.InvalidColumnInUpsertOption);
            }
        }

    }

    private static boolean isInvalidURL(String configURL) {
        try {
            URL url = new URL(configURL);
            if (!url.getProtocol().equals("https")) throw new Exception();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    public static void validateGetByIdRequestRecord(GetByIdRecordInput record) throws SkyflowException {
        String table = record.getTable();

        if (table == null || table.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.InvalidTable.getLog());
            throw new SkyflowException(ErrorCode.InvalidTable);
        }
    }

    public static void validateGetRequestRecord(GetRecordInput record) throws SkyflowException {
        String[] ids = record.getIds();
        String table = record.getTable();
        String columnName = record.getColumnName();
        String[] columnValues = record.getColumnValues();

        if (table == null || table.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.InvalidTable.getLog());
            throw new SkyflowException(ErrorCode.InvalidTable);
        }

        if (ids == null && columnName == null && columnValues == null) {
            LogUtil.printErrorLog(ErrorLogs.MissingIdAndColumnName.getLog());
            throw new SkyflowException(ErrorCode.MissingIdAndColumnName);
        }

        if (columnName != null && columnValues == null) {
            LogUtil.printErrorLog(ErrorLogs.MissingRecordColumnValue.getLog());
            throw new SkyflowException(ErrorCode.MissingRecordColumnValue);
        }

        if (columnName == null && columnValues != null) {
            LogUtil.printErrorLog(ErrorLogs.MissingRecordColumnName.getLog());
            throw new SkyflowException(ErrorCode.MissingRecordColumnName);
        }
    }
}
