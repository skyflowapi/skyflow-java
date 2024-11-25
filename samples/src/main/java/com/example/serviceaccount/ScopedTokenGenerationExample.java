package com.example.serviceaccount;

import com.skyflow.errors.SkyflowException;
import com.skyflow.serviceaccount.util.BearerToken;

import java.io.File;
import java.util.ArrayList;

public class ScopedTokenGenerationExample {
    public static void main(String[] args) {
        String scopedToken = null;

        // Generate Scoped Token  by specifying credentials.json file path
        try {
            ArrayList<String> roles = new ArrayList<>();
            roles.add("YOUR_ROLE_ID");
            String filePath = "<YOUR_CREDENTIALS_FILE_PATH>";
            BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(new File(filePath))
                    .setRoles(roles)
                    .build();

            scopedToken = bearerToken.getBearerToken();
            System.out.println(scopedToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }

        // Generate BearerToken with context by specifying credentials.json as string
        try {
            ArrayList<String> roles = new ArrayList<>();
            roles.add("YOUR_ROLE_ID");
            String fileContents = "<YOUR_CREDENTIALS_FILE_CONTENTS_AS_STRING>";
            BearerToken bearerToken = BearerToken.builder()
                    .setCredentials(fileContents)
                    .setRoles(roles)
                    .build();

            scopedToken = bearerToken.getBearerToken();
            System.out.println(scopedToken);
        } catch (SkyflowException e) {
            e.printStackTrace();
        }
    }
}