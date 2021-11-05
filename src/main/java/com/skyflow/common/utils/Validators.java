package com.skyflow.common.utils;

import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;

import java.net.URL;

public class Validators {
    public static void validateConfiguration(SkyflowConfiguration config) throws SkyflowException {
        if (config.getVaultID().length() <= 0)
            throw new SkyflowException(ErrorCode.EmptyVaultID);

        try {
            URL url = new URL(config.getVaultURL());
            if (!url.getProtocol().equals("https")) throw new Exception();
        } catch (Exception e) {
            throw new SkyflowException(ErrorCode.InvalidVaultURL);
        }
    }
}