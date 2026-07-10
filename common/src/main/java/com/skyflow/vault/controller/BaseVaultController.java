package com.skyflow.vault.controller;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.logger.LogUtil;

import java.util.List;
import java.util.Map;

public class BaseVaultController {
    protected static final Gson GSON = new GsonBuilder().serializeNulls().create();

    protected static SkyflowException wrapApiException(int statusCode, Throwable cause,
                                                         Map<String, List<String>> headers,
                                                         Object responseBody, ErrorLogs errorLog) {
        LogUtil.printErrorLog(errorLog.getLog());
        return new SkyflowException(statusCode, cause, headers, GSON.toJson(responseBody));
    }
}
