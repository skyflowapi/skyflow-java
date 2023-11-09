/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.common.utils;

import com.skyflow.entities.TokenProvider;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import org.apache.commons.codec.binary.Base64;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public final class TokenUtils {

    private String token;

    public TokenUtils() {
        LogUtil.printInfoLog(InfoLogs.TokenUtilsInstanceCreated.getLog());
        this.token = null;
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

    private static boolean isExpired(String encodedToken) throws ParseException, SkyflowException {
        long currentTime = new Date().getTime() / 1000;
        long expiryTime = (long) decoded(encodedToken).get("exp");
        return currentTime > expiryTime;
    }

    public static JSONObject decoded(String encodedToken) throws ParseException, SkyflowException {
        String[] split = encodedToken.split("\\.");
        if (split.length < 3) {
            throw new SkyflowException(ErrorCode.InvalidBearerToken);
        }
        byte[] decodedBytes = Base64.decodeBase64(split[1]);
        return (JSONObject) new JSONParser().parse(new String(decodedBytes, StandardCharsets.UTF_8));
    }

    public String getBearerToken(TokenProvider tokenProvider) throws SkyflowException {
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
}
