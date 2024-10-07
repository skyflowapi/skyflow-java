package com.skyflow.serviceaccount.util;

import com.skyflow.errors.SkyflowException;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Token {
    public static boolean isExpired(String token) {
        long expiryTime;
        long currentTime;
        try {
            if (token == null || token.isEmpty()) {
                // print info log
                return true;
            }

            currentTime = new Date().getTime() / 1000;
            expiryTime = (long) decoded(token).get("exp");

        } catch (ParseException e) {
            // display info log
            return true;
        } catch (SkyflowException e) {
            // display info log
            return true;
        }
        return currentTime > expiryTime;
    }

    static JSONObject decoded(String encodedToken) throws ParseException, SkyflowException {
        String[] split = encodedToken.split("\\.");
        if (split.length < 3) {
            throw new SkyflowException("invalid bearer token");
        }
        byte[] decodedBytes = Base64.decodeBase64(split[1]);
        return (JSONObject) new JSONParser().parse(new String(decodedBytes, StandardCharsets.UTF_8));
    }
}
