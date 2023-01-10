/*
	Copyright (c) 2022 Skyflow, Inc.
*/
package com.example;

import com.skyflow.serviceaccount.util.BearerToken;
import java.io.File;

public class ScopedTokenGenerationExample {
    public static void main(String args[]) {

        String scopedToken = null;

        // Generate Scoped Token  by specifying credentials.json file path
        try {
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken token = new BearerToken.BearerTokenBuilder()
                    .setCredentials(new File(filePath))
                    .setRoles(new String[]{"roleID"})
                    .build();

            scopedToken = token.getBearerToken();
            System.out.println(scopedToken);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
