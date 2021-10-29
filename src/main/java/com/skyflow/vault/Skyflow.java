package com.skyflow.vault;


import com.skyflow.common.utils.Validators;
import com.skyflow.entities.SkyflowConfiguration;
import com.skyflow.errors.SkyflowException;

public class Skyflow {
    private final SkyflowConfiguration configuration;

    private Skyflow(SkyflowConfiguration config) {
        this.configuration = config;
    }

    public static Skyflow init(SkyflowConfiguration clientConfig) throws SkyflowException {
        Validators.validateConfiguration(clientConfig);
        return new Skyflow(clientConfig);
    }
}
