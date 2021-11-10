package com.skyflow.common.utils;

import com.skyflow.entities.RequestMethod;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;

import java.net.URL;

public class Validators {
    public static void validateConfiguration(SkyflowConfiguration config) throws SkyflowException {
        if (config.getVaultID().length() <= 0)
            throw new SkyflowException(ErrorCode.EmptyVaultID);

        if (isInvalidURL(config.getVaultURL()))
            throw new SkyflowException(ErrorCode.InvalidVaultURL);

    }

    public static void validateConnectionConfiguration(JSONObject connectionConfig) throws SkyflowException {
        if (connectionConfig.containsKey("connectionURL")) {
            String connectionURL = (String) connectionConfig.get("connectionURL");
            if (isInvalidURL(connectionURL))
                throw new SkyflowException(ErrorCode.InvalidConnectionURL);
        } else {
            throw new SkyflowException(ErrorCode.ConnectionURLMissing);
        }

        if (connectionConfig.containsKey("methodName")) {
            try {
                RequestMethod requestMethod = (RequestMethod) connectionConfig.get("methodName");
            } catch (Exception e) {
                throw new SkyflowException(ErrorCode.InvalidMethodName);
            }
        } else {
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
