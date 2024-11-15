package com.skyflow.serviceaccount.util;

import com.skyflow.utils.Constants;

public class SignedDataTokenResponse {
    private static final String prefix = Constants.SIGNED_DATA_TOKEN_PREFIX;
    private final String dataToken;
    private final String signedDataToken;

    public SignedDataTokenResponse(String dataToken, String signedDataToken) {
        this.dataToken = dataToken;
        this.signedDataToken = new StringBuilder(prefix).append(signedDataToken).toString();
    }

    @Override
    public String toString() {
        return "{" +
                "\n\t\"dataToken\":\"" + this.dataToken + "\"," +
                "\n\t\"signedDataToken\":\"" + this.signedDataToken + "\"," +
                "\n}";
    }
}
