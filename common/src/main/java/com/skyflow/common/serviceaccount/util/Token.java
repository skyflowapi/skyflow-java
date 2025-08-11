package com.skyflow.common.serviceaccount.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.common.logs.ErrorLogs;
import com.skyflow.common.logs.InfoLogs;
import com.skyflow.common.utils.logger.LogUtil;
import org.apache.commons.codec.binary.Base64;

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
            expiryTime = decoded(token).get("exp").getAsLong();

        } catch (JsonSyntaxException | SkyflowException e) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
            return true;
        }
        return currentTime > expiryTime;
    }

    static JsonObject decoded(String encodedToken) throws JsonSyntaxException, SkyflowException {
        String[] split = encodedToken.split("\\.");
        if (split.length < 3) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_BEARER_TOKEN.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.JwtDecodeError.getMessage());
        }
        byte[] decodedBytes = Base64.decodeBase64(split[1]);
        return JsonParser.parseString(new String(decodedBytes, StandardCharsets.UTF_8)).getAsJsonObject();
    }
}
