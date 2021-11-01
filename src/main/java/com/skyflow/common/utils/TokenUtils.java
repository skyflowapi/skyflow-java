package com.skyflow.common.utils;

import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.SkyflowException;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Date;

import static com.skyflow.errors.ErrorCodesEnum.InvalidInput;

public class TokenUtils {

    private static String token = null;

    public static String getBearerToken(TokenProvider tokenProvider) throws SkyflowException {
        if (token != null && isTokenValid(token))
            return token;

        token = tokenProvider.getBearerToken();
        return token;
    }

    public static boolean isTokenValid(String token) throws SkyflowException {
        try {
            if (token != null) {
                return !isExpired(token);
            }
        } catch (ParseException e) {
            throw new SkyflowException(InvalidInput, String.format("Invalid token"), e);
        }

        return false;
    }

    public static boolean isExpired(String encodedToken) throws ParseException {
        long currentTime = new Date().getTime() / 1000;
        long expiryTime = (long) decoded(encodedToken).get("exp");
        return currentTime > expiryTime;
    }

    public static JSONObject decoded(String encodedToken) throws ParseException {
        String[] split = encodedToken.split("\\.");
        byte[] decodedBytes = Base64.getDecoder().decode(split[1]);
        return (JSONObject) new JSONParser().parse(new String(decodedBytes, StandardCharsets.UTF_8));
    }

}
