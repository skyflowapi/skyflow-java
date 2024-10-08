package com.skyflow.utils.validations;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.tokens.DetokenizeRequest;

// Add config and request validations
public class Validations {

    public static void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {

    }

    public static void validateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {

    }

    public static void validateCredentials(Credentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        if (credentials.getPath() != null) nonNullMembers++;
        if (credentials.getCredentialsString() != null) nonNullMembers++;
        if (credentials.getToken() != null) nonNullMembers++;

        if (nonNullMembers != 1) {
            throw new SkyflowException("only one of tokens, path, and credentialsString is allowed");
        }
    }

    public static void validateDetokenizeRequest(DetokenizeRequest detokenizeRequest) throws SkyflowException {

    }

    public static void validateInsertRequest(InsertRequest insertRequest) throws SkyflowException {

    }
}
