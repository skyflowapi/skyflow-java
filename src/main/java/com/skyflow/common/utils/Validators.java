package com.skyflow.common.utils;

import com.skyflow.entities.RequestMethod;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import org.json.simple.JSONObject;

import java.net.URL;

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

    }

    public static void validateConnectionConfiguration(JSONObject connectionConfig) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.ValidatingInvokeConnectionConfig.getLog());
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

    private static boolean isInvalidURL(String configURL) {
        try {
            URL url = new URL(configURL);
            if (!url.getProtocol().equals("https")) throw new Exception();
        } catch (Exception e) {
            return true;
        }
        return false;
    }
}
