package com.skyflow.serviceaccount.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import java.util.Base64;

import java.nio.charset.StandardCharsets;
import java.util.Date;

public class Token {
    public static boolean isExpired(String token) {
        long expiryTime;
        long currentTime;
        try {
            if (token == null || token.trim().isEmpty()) {
                LogUtil.printInfoLog(InfoLogs.EMPTY_BEARER_TOKEN.getLog());
                return true;
            }

            currentTime = new Date().getTime() / 1000;
            JsonElement exp = decoded(token).get("exp");
            if (exp == null) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.JwtDecodeError.getMessage());
            }
            expiryTime = exp.getAsLong();

        } catch (JsonSyntaxException | SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
            return true;
        }
        return currentTime > expiryTime;
    }

    /**
     * Returns true when the token's {@code exp} claim is at or before now plus {@code windowSeconds},
     * that is, the token has expired or will within the window. Unlike {@link #isExpired(String)},
     * a token that is empty, cannot be decoded, or carries no {@code exp} is reported as
     * <em>not</em> expiring: callers use this to decide whether a {@code 401} was caused by expiry,
     * and a malformed token is not an expiry.
     */
    public static boolean isExpiringWithin(String token, long windowSeconds) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        try {
            JsonElement exp = decoded(token).get("exp");
            if (exp == null) {
                return false;
            }
            long currentTime = new Date().getTime() / 1000;
            return exp.getAsLong() <= currentTime + windowSeconds;
        } catch (SkyflowException | RuntimeException e) {
            return false;
        }
    }

    static JsonObject decoded(String encodedToken) throws JsonSyntaxException, SkyflowException {
        String[] split = encodedToken.split("\\.");
        if (split.length < 3) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.JwtDecodeError.getMessage());
        }
        try {
            byte[] decodedBytes = Base64.getUrlDecoder().decode(split[1]);
            return JsonParser.parseString(new String(decodedBytes, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IllegalArgumentException | IllegalStateException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.JwtDecodeError.getMessage());
        }
    }
}
