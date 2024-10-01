package com.skyflow.utils;

import com.skyflow.config.Credentials;
import com.skyflow.enums.ENV;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.auth.HttpBearerAuth;

public class Utils {
    public static String getVaultURL(String clusterId, ENV env) {
        switch (env) {
            case DEV:
                return "https://" + clusterId + ".vault.skyflowapis.dev";
            case STAGE:
                return "https://" + clusterId + ".vault.skyflowapis.tech";
            case SANDBOX:
                return "https://" + clusterId + ".vault.skyflowapis-preview.com";
            case PROD:
            default:
                return "https://" + clusterId + ".vault.skyflowapis.com";
        }
    }

    public static void updateBearerTokenIfExpired(ApiClient apiClient, Credentials credentials) {
        HttpBearerAuth Bearer = (HttpBearerAuth) apiClient.getAuthentication("Bearer");
        // check validity of bearer token
        // String token = Bearer.getBearerToken();
        // generate bearer token, set in ApiClient if expired or null
        Bearer.setBearerToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL21hbmFnZS5za3lmbG93YXBpcy50ZWNoIiwiY2xpIjoiYzFiMWZjODViYWEzNDk0NGJkZGNkMzQzMTA2YzIyOGQiLCJleHAiOjE3Mjc3ODk0OTUsImlhdCI6MTcyNzc4NTg5NiwiaXNzIjoic2EtYXV0aEBtYW5hZ2Uuc2t5Zmxvd2FwaXMudGVjaCIsImp0aSI6InQxNjhjNTlhYWIzNDRiZTZiMDE0ZWY3MzY4MDlhMjZhIiwia2V5IjoiamI4ODgxZjRiYTM5NDg0YjkzYzY5MzVjMjhhODg5MDgiLCJzY3AiOm51bGwsInN1YiI6IlNES19FMkUifQ.EmCYoR1ieDdseC4XTWOA9oBD7EJPSd5kz4Oka7mPFqLNW4Ms21GMhXll66Q3seKXZH-_DiLR8d3juGSx3nnZZDaAmNANiPfrvNCF66LU-587RDzzIowL-I5qhGigGG8dLh2FICf0eEoRAvdtiYSolWjErbhd-o5k1whxdCZ2cU_8JHiqq3hr48Gy1pVcFYycJ_oj0bxrOqn-r3vfdv_ChruLRQ2xh74qevkzEePg3XSkuniVDemhPp_fVOW4RE6TGYo6vEaWsY6NoPqx21CL49U8avqkIISZm7Uo5PO-sfaRDh3XCy-QFvvR7KoWiMTGj8cHcIkV5uusCV-S8CFIvQ");
    }
}
