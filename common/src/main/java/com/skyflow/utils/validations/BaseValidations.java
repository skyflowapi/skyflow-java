package com.skyflow.utils.validations;

import com.skyflow.config.BaseCredentials;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.BaseConstants;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class BaseValidations {
    BaseValidations() {
    }

    public static void validateCredentials(BaseCredentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        String path = credentials.getPath();
        String credentialsString = credentials.getCredentialsString();
        String token = credentials.getToken();
        String apiKey = credentials.getApiKey();
        Object context = credentials.getContext();
        ArrayList<String> roles = credentials.getRoles();

        if (path != null) nonNullMembers++;
        if (credentialsString != null) nonNullMembers++;
        if (token != null) nonNullMembers++;
        if (apiKey != null) nonNullMembers++;

        if (nonNullMembers > 1) {
            LogUtil.printErrorLog(ErrorLogs.MULTIPLE_TOKEN_GENERATION_MEANS_PASSED.getLog());
            throw new SkyflowException(
                    ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MultipleTokenGenerationMeansPassed.getMessage()
            );
        } else if (nonNullMembers < 1) {
            LogUtil.printErrorLog(ErrorLogs.NO_TOKEN_GENERATION_MEANS_PASSED.getLog());
            throw new SkyflowException(
                    ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NoTokenGenerationMeansPassed.getMessage()
            );
        } else if (path != null && path.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CREDENTIALS_PATH.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentialFilePath.getMessage());
        } else if (credentialsString != null && credentialsString.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CREDENTIALS_STRING.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentialsString.getMessage());
        } else if (token != null && token.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_TOKEN_VALUE.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyToken.getMessage());
        } else if (apiKey != null) {
            if (apiKey.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_API_KEY_VALUE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyApikey.getMessage());
            } else {
                Pattern pattern = Pattern.compile(BaseConstants.API_KEY_REGEX);
                Matcher matcher = pattern.matcher(apiKey);
                if (!matcher.matches()) {
                    LogUtil.printErrorLog(ErrorLogs.INVALID_API_KEY.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidApikey.getMessage());
                }
            }
        } else if (roles != null) {
            if (roles.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_ROLES.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRoles.getMessage());
            } else {
                for (int index = 0; index < roles.size(); index++) {
                    String role = roles.get(index);
                    if (role == null || role.trim().isEmpty()) {
                        LogUtil.printErrorLog(BaseUtils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_ROLE_IN_ROLES.getLog(), Integer.toString(index)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRoleInRoles.getMessage());
                    }
                }
            }
        }
        if (context != null) {
            if (context instanceof String) {
                String ctxStr = (String) context;
                if (ctxStr.trim().isEmpty()) {
                    LogUtil.printErrorLog(ErrorLogs.EMPTY_OR_NULL_CONTEXT.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyContext.getMessage());
                }
            } else if (context instanceof Map) {
                Map<?, ?> ctxMap = (Map<?, ?>) context;
                if (ctxMap.isEmpty()) {
                    LogUtil.printErrorLog(ErrorLogs.EMPTY_OR_NULL_CONTEXT.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyContext.getMessage());
                }
                Pattern ctxKeyPattern = Pattern.compile(BaseConstants.CONTEXT_KEY_REGEX);
                for (Object key : ctxMap.keySet()) {
                    if (key == null || !ctxKeyPattern.matcher(key.toString()).matches()) {
                        String keyStr = key == null ? "null" : key.toString();
                        LogUtil.printErrorLog(BaseUtils.parameterizedString(
                                ErrorLogs.INVALID_CONTEXT_MAP_KEY.getLog(), keyStr));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                                BaseUtils.parameterizedString(ErrorMessage.InvalidContextMapKey.getMessage(), keyStr));
                    }
                }
            } else {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CONTEXT_TYPE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidContextType.getMessage());
            }
        }
    }
}
