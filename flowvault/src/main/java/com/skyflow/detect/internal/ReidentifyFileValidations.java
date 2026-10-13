package com.skyflow.detect.internal;

import com.skyflow.detect.ReidentifyFileRequest;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;


/**
 * Client-side validation for the flowvault {@code reidentifyFile} request. Internal to the SDK.
 */
public final class ReidentifyFileValidations {
    private static final String INTERFACE_NAME = "detect";

    private ReidentifyFileValidations() {
    }

    public static void validateReidentifyFileRequest(ReidentifyFileRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.REIDENTIFY_FILE_REQUEST_NULL, ErrorMessage.ReidentifyFileRequestNull);
        }
        if (request.getDataSource() == null) {
            throw invalid(ErrorLogs.INVALID_DATA_SOURCE_IN_REIDENTIFY_FILE, ErrorMessage.InvalidDataSourceInReidentifyFile);
        }
        if (isBlank(request.getValue())) {
            throw invalid(ErrorLogs.INVALID_VALUE_IN_REIDENTIFY_FILE, ErrorMessage.InvalidValueInReidentifyFile);
        }
        // redactionLevel entries are optional on the API and are passed through as given.
    }


    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static SkyflowException invalid(ErrorLogs log, ErrorMessage message) {
        LogUtil.printErrorLog(BaseUtils.parameterizedString(log.getLog(), INTERFACE_NAME));
        return new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), message.getMessage());
    }
}
