package com.skyflow.serviceaccount.util;

import com.google.gson.Gson;
import com.skyflow.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;

public class SignedDataTokenResponse {
    private static final String prefix = Constants.SIGNED_DATA_TOKEN_PREFIX;
    private final String token;
    private final String signedToken;
    private final ArrayList<HashMap<String, Object>> errors;

    public SignedDataTokenResponse(String token, String signedToken, ArrayList<HashMap<String, Object>> errors) {
        this.token = token;
        this.signedToken = new StringBuilder(prefix).append(signedToken).toString();
        this.errors = errors;
    }

    public String getToken() {
        return token;
    }

    public String getSignedToken() {
        return signedToken;
    }

    public ArrayList<HashMap<String, Object>> getErrors() {
        return errors;
    }

    @Override
    public String toString() {
        Gson gson = new Gson().newBuilder().serializeNulls().create();
        return gson.toJson(this);
    }
}