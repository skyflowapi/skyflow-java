/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Contains the parameters required for Skyflow client initialisation.
 */
public final class SkyflowConfiguration {
    private final String vaultID;
    private final String vaultURL;
    private final TokenProvider tokenProvider;

    /**
     *
     * @param vaultID Unique ID of a vault.
     * @param vaultURL URL of the vault.
     * @param tokenProvider An implementation of the token provider interface.
     */
    public SkyflowConfiguration(String vaultID, String vaultURL, TokenProvider tokenProvider) {
        this.vaultID = vaultID;
        this.vaultURL = formatVaultURL(vaultURL);
        this.tokenProvider = tokenProvider;
    }
    /**
     * @ignore
     */
    public SkyflowConfiguration(TokenProvider tokenProvider){
        this.vaultID = "";
        this.vaultURL = "";
        this.tokenProvider = tokenProvider;
    }

    /**
     * Fetches the vault ID.
     * @return Returns the vault ID.
     */
    public String getVaultID() {
        return vaultID;
    }

    /**
     * Fetches the vault URL.
     * @return Returns the vault URL.
     */
    public String getVaultURL() {
        return vaultURL;
    }

    /**
     * Fetches the token provider.
     * @return Returns the token provider.
     */
    public TokenProvider getTokenProvider() {
        return tokenProvider;
    }

    private String formatVaultURL(String vaultURL){
        if(vaultURL != null && vaultURL.trim().length() > 0 && vaultURL.trim().charAt(vaultURL.trim().length() - 1) == '/')
                return vaultURL.trim().substring(0,vaultURL.trim().length()-1);
        return vaultURL;
    }
}