package com.skyflow.utils;

import com.skyflow.config.Credentials;
import com.skyflow.enums.ENV;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.auth.HttpBearerAuth;

public class Utils {
    public static String getVaultURL(String clusterId, ENV env) {
        StringBuilder sb = new StringBuilder(Constants.SECURE_PROTOCOL);
        sb.append(clusterId);
        switch (env) {
            case DEV:
                sb.append(Constants.DEV_DOMAIN);
                break;
            case STAGE:
                sb.append(Constants.STAGE_DOMAIN);
                break;
            case SANDBOX:
                sb.append(Constants.SANDBOX_DOMAIN);
                break;
            case PROD:
            default:
                sb.append(Constants.PROD_DOMAIN);
                break;
        }
        return sb.toString();
    }

    public static void updateBearerTokenIfExpired(ApiClient apiClient, Credentials credentials) {
        HttpBearerAuth Bearer = (HttpBearerAuth) apiClient.getAuthentication("Bearer");
        // check validity of bearer token
        // String token = Bearer.getBearerToken();
        // generate bearer token, set in ApiClient if expired or null
        Bearer.setBearerToken("BEARER_TOKEN");
    }
}
