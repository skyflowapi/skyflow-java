/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.skyflow.serviceaccount.util;

public class SignedDataTokenResponse {
    String dataToken;
    String signedDataToken;

    public SignedDataTokenResponse(String dataToken, String signedDataToken) {
        this.dataToken = dataToken;
        this.signedDataToken = signedDataToken;
    }

    @Override
    public String toString() {
        return "{" + "dataToken: " + dataToken + "," + "signedDataToken: " + signedDataToken + "}";

    }
}
