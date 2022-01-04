package com.skyflow.entities;

public final class SkyflowConfiguration {
    private final String vaultID;
    private final String vaultURL;
    private final TokenProvider tokenProvider;

    /**
     *
     * @param vaultID is the Skyflow vaultID, which can be found in EditVault Details.
     * @param vaultURL is the vaultURL, which can be found in EditVault Details.
     * @param tokenProvider class which implements the TokenProvider interface.
     */
    public SkyflowConfiguration(String vaultID, String vaultURL, TokenProvider tokenProvider) {
        this.vaultID = vaultID;
        this.vaultURL = formatVaultURL(vaultURL);
        this.tokenProvider = tokenProvider;
    }
    public SkyflowConfiguration(TokenProvider tokenProvider){
        this.vaultID = "";
        this.vaultURL = "";
        this.tokenProvider = tokenProvider;
    }


    public String getVaultID() {
        return vaultID;
    }

    public String getVaultURL() {
        return vaultURL;
    }

    public TokenProvider getTokenProvider() {
        return tokenProvider;
    }

    private String formatVaultURL(String vaultURL){
        if(vaultURL != null && vaultURL.trim().length() > 0 && vaultURL.trim().charAt(vaultURL.trim().length() - 1) == '/')
                return vaultURL.trim().substring(0,vaultURL.trim().length()-1);
        return vaultURL;
    }
}