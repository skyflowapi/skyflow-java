package com.skyflow.entities;

public class SkyflowConfiguration {
    private final String vaultID;
    private final String vaultURL;
    private final TokenProvider tokenProvider;
//    private final Options options;

    /**
     *
     * @param vaultID is the Skyflow vaultID, which can be found in EditVault Details.
     * @param vaultURL is the vaultURL, which can be found in EditVault Details.
     * @param tokenProvider class which implements the TokenProvider interface.
     */
    public SkyflowConfiguration(String vaultID, String vaultURL, TokenProvider tokenProvider) {
        this.vaultID = vaultID;
        this.vaultURL = vaultURL;
        this.tokenProvider = tokenProvider;
//        this.options = new Options(LogLevel.ERROR);
    }

//    public SkyflowConfiguration(String vaultID, String vaultURL, TokenProvider tokenProvider, Options options) {
//        this.vaultID = vaultID;
//        this.vaultURL = vaultURL;
//        this.tokenProvider = tokenProvider;
//        this.options = options;
//    }

    public String getVaultID() {
        return vaultID;
    }

    public String getVaultURL() {
        return vaultURL;
    }

    public TokenProvider getTokenProvider() {
        return tokenProvider;
    }

//    public Options getOptions() {
//        return options;
//    }

}
