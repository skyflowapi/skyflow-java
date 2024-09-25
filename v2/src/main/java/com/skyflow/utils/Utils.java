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
        String token = Bearer.getBearerToken();
        // generate bearer token, set in ApiClient if expired or null
        Bearer.setBearerToken("eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.eyJhdWQiOiJodHRwczovL21hbmFnZS5za3lmbG93YXBpcy50ZWNoIiwiY2xpIjoiYzFiMWZjODViYWEzNDk0NGJkZGNkMzQzMTA2YzIyOGQiLCJleHAiOjE3MjcyNTYyMzMsImlhdCI6MTcyNzI1MjYzNCwiaXNzIjoic2EtYXV0aEBtYW5hZ2Uuc2t5Zmxvd2FwaXMudGVjaCIsImp0aSI6Imc4ZDM3MGI2MmQ0NzQ0M2JhMjdjYWIxMzYxYmU1YjI3Iiwia2V5IjoiamI4ODgxZjRiYTM5NDg0YjkzYzY5MzVjMjhhODg5MDgiLCJzY3AiOm51bGwsInN1YiI6IlNES19FMkUifQ.kbWIbPvjqnawofNgBFwAvnBknaj5_Qo4Gpav9M9XCriOhJdBgPXP9Lc7o4rPrMmmxsI_Lsj3AIK4HGzBvKiA7jLeFOFgjNuHqBJLKzwdixURnYde3bk-7aMUmMWfkg7tzz5uxhcAfgQMrEfGhxdAIzJ_7xSBjaF_2Znz9fPY9_oDSmY1gf4NI-GWZVIx6fEdGxkEWE0W-QNl9EuEeGkGaw40il77Py0XmH8RarT8aNzsdXiTIWDqzW8McdHMiB0R99ceRPAm-a2V8HJC5FyNOEKb6uSeM4ZEz05HmAgxATTAROwL0SlRiFSDG8zh1ER4pO3QJIEIIFUkX9UNqncNuA");
    }
}
