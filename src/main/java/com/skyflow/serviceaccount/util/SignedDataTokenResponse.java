package com.skyflow.serviceaccount.util;

import com.google.gson.Gson;
import com.skyflow.utils.Constants;

public class SignedDataTokenResponse {
    private static final String prefix = Constants.SIGNED_DATA_TOKEN_PREFIX;
    private final String token;
    private final String signedToken;

    public SignedDataTokenResponse(String token, String signedToken) {
        this.token = token;
        this.signedToken = new StringBuilder(prefix).append(signedToken).toString();
    }

    public String getToken() {
        return token;
    }

    public String getSignedToken() {
        return signedToken;
    }

    @Override
    public String toString() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
