package com.skyflow.detect.internal;

import com.skyflow.detect.CheckGuardrailsRequest;
import com.skyflow.detect.DeidentifyFileRequest;
import com.skyflow.detect.DeidentifyStringRequest;
import com.skyflow.detect.PollOptions;
import com.skyflow.detect.GetRunRequest;
import com.skyflow.detect.ReidentifyStringRequest;
import com.skyflow.errors.BaseErrorMessage;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.BaseUtils;
import com.skyflow.utils.logger.LogUtil;


/**
 * Client-side validation for Detect V2 requests. Internal to the SDK.
 */
public final class DetectValidations {
    static final String INTERFACE_NAME = "detect";

    private DetectValidations() {
    }

    public static void validateDeidentifyStringRequest(DeidentifyStringRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.DEIDENTIFY_STRING_REQUEST_NULL, BaseErrorMessage.DeidentifyStringRequestNull);
        }
        if (isBlank(request.getText())) {
            throw invalid(ErrorLogs.INVALID_TEXT_IN_DEIDENTIFY_STRING, BaseErrorMessage.InvalidTextInDeidentifyString);
        }
        // configurationId and configuration are both optional on the API and are passed through as given.
    }

    public static void validateDeidentifyFileRequest(DeidentifyFileRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.DEIDENTIFY_FILE_REQUEST_NULL, BaseErrorMessage.DeidentifyFileRequestNull);
        }
        if (request.getFile() != null) {
            if (!isBlank(request.getValue())) {
                throw invalid(ErrorLogs.MULTIPLE_FILE_SOURCES_IN_DEIDENTIFY_FILE, BaseErrorMessage.MultipleFileSourcesInDeidentifyFile);
            }
            if (!request.getFile().isFile()) {
                throw invalid(ErrorLogs.FILE_NOT_FOUND_IN_DEIDENTIFY_FILE, BaseErrorMessage.FileNotFoundToDeidentify);
            }
        } else {
            if (request.getDataSource() == null) {
                throw invalid(ErrorLogs.INVALID_DATA_SOURCE_IN_DEIDENTIFY_FILE, BaseErrorMessage.InvalidDataSourceInDeidentifyFile);
            }
            if (isBlank(request.getValue())) {
                throw invalid(ErrorLogs.INVALID_VALUE_IN_DEIDENTIFY_FILE, BaseErrorMessage.InvalidValueInDeidentifyFile);
            }
        }
        // configurationId and configuration are both optional on the API and are passed through as given.
        validatePollOptions(request.getPollOptions());
        if (request.getPollOptions() == null && !isBlank(request.getOutputDirectory())) {
            throw invalid(ErrorLogs.OUTPUT_DIRECTORY_WITHOUT_POLL_OPTIONS, BaseErrorMessage.OutputDirectoryWithoutPollOptions);
        }
    }

    static void validatePollOptions(PollOptions options) throws SkyflowException {
        if (options == null) {
            return;
        }
        Integer waitTime = options.getWaitTime();
        if (waitTime != null && (waitTime < 1 || waitTime > PollOptions.MAX_WAIT_TIME_SECONDS)) {
            throw invalid(ErrorLogs.INVALID_WAIT_TIME_IN_POLL_OPTIONS, BaseErrorMessage.InvalidWaitTimeInPollOptions);
        }
        Integer maxAttempts = options.getMaxAttempts();
        if (maxAttempts != null && (maxAttempts < 1 || maxAttempts > PollOptions.MAX_MAX_ATTEMPTS)) {
            throw invalid(ErrorLogs.INVALID_MAX_ATTEMPTS_IN_POLL_OPTIONS, BaseErrorMessage.InvalidMaxAttemptsInPollOptions);
        }
    }

    public static void validateReidentifyStringRequest(ReidentifyStringRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.REIDENTIFY_STRING_REQUEST_NULL, BaseErrorMessage.ReidentifyStringRequestNull);
        }
        if (isBlank(request.getText())) {
            throw invalid(ErrorLogs.INVALID_TEXT_IN_REIDENTIFY_STRING, BaseErrorMessage.InvalidTextInReidentifyString);
        }
        // redactionLevel entries are optional on the API and are passed through as given.
    }

    public static void validateGetRunRequest(GetRunRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.GET_RUN_REQUEST_NULL, BaseErrorMessage.GetRunRequestNull);
        }
        if (isBlank(request.getRunId())) {
            throw invalid(ErrorLogs.INVALID_RUN_ID_IN_GET_RUN, BaseErrorMessage.InvalidRunIdInGetRun);
        }
    }

    public static void validateCheckGuardrailsRequest(CheckGuardrailsRequest request) throws SkyflowException {
        if (request == null) {
            throw invalid(ErrorLogs.CHECK_GUARDRAILS_REQUEST_NULL, BaseErrorMessage.CheckGuardrailsRequestNull);
        }
        if (isBlank(request.getText())) {
            throw invalid(ErrorLogs.INVALID_TEXT_IN_CHECK_GUARDRAILS, BaseErrorMessage.InvalidTextInCheckGuardrails);
        }
        // checkToxicity and denyTopics are optional on the API and are passed through as given.
    }


    static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private static SkyflowException invalid(ErrorLogs log, BaseErrorMessage message) {
        LogUtil.printErrorLog(BaseUtils.parameterizedString(log.getLog(), INTERFACE_NAME));
        return new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), message.getMessage());
    }
}
