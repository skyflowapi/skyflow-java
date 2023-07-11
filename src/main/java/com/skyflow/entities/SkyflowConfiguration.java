/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * This is the description for SkyflowConfiguration Class.
 */
public final class SkyflowConfiguration {
    private final String vaultID;
    private final String vaultURL;
    private final TokenProvider tokenProvider;

    /**
     *
     * @param vaultID vaultID is the Skyflow vaultID, which can be found in EditVault Details.
     * @param vaultURL vaultURL is the vaultURL, which can be found in EditVault Details.
     * @param tokenProvider tokenProvider class which implements the TokenProvider interface.
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
     * This is the description for getVaultID method.
     * @return This is the description of what the method returns.
     */
    public String getVaultID() {
        return vaultID;
    }

    /**
     * This is the description for getVaultURL method.
     * @return This is the description of what the method returns.
     */
    public String getVaultURL() {
        return vaultURL;
    }

    /**
     * This is the description for getTokenProvider method.
     * @return This is the description of what the method returns.
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