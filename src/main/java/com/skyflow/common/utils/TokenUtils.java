package com.skyflow.common.utils;

import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class TokenUtils {

    private static String token = null;

    public static String getBearerToken(TokenProvider tokenProvider) throws SkyflowException {
        if (token != null && isTokenValid(token))
            return token;

        try {
            token = tokenProvider.getBearerToken();
            isTokenValid(token);
        } catch (SkyflowException exception) {
            throw exception;
        } catch (Exception e) {
            LogUtil.printErrorLog(Helpers.parameterizedString(ErrorLogs.BearerThrownException.getLog(), e.getMessage()));
            throw new SkyflowException(ErrorCode.BearerThrownException.getCode(), e.getMessage(), e);
        }
        return token;
    }

    public static boolean isTokenValid(String token) throws SkyflowException {
        try {
            if (token != null) {
                return !isExpired(token);
            }
        } catch (ParseException e) {
            LogUtil.printErrorLog(ErrorLogs.InvalidBearerToken.getLog());
            throw new SkyflowException(ErrorCode.InvalidBearerToken, e);
        }

        return false;
    }

    private static boolean isExpired(String encodedToken) throws ParseException {
        long currentTime = new Date().getTime() / 1000;
        currentTime = currentTime - 300;
        long expiryTime = (long) decoded(encodedToken).get("exp");
        return currentTime > expiryTime;
    }

    public static JSONObject decoded(String encodedToken) throws ParseException {
        String[] split = encodedToken.split("\\.");
        byte[] decodedBytes = Base64.decodeBase64(split[1]);
        return (JSONObject) new JSONParser().parse(new String(decodedBytes, StandardCharsets.UTF_8));
    }

}
