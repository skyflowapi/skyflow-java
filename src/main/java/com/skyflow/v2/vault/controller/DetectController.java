package com.skyflow.v2.vault.controller;

import com.google.gson.*;
import com.skyflow.v2.VaultClient;
import com.skyflow.common.config.Credentials;
import com.skyflow.common.config.VaultConfig;
import com.skyflow.v2.enums.DeidentifyFileStatus;
import com.skyflow.common.errors.ErrorCode;
import com.skyflow.common.errors.ErrorMessage;
import com.skyflow.common.errors.SkyflowException;
import com.skyflow.common.generated.core.ApiClientApiException;
import com.skyflow.v2.generated.rest.resources.files.requests.*;
import com.skyflow.v2.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.v2.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.v2.generated.rest.types.*;
import com.skyflow.common.logs.ErrorLogs;
import com.skyflow.common.logs.InfoLogs;
import com.skyflow.v2.utils.Constants;
import com.skyflow.common.logger.LogUtil;
import com.skyflow.v2.utils.validations.Validations;
import com.skyflow.v2.vault.detect.*;
import com.skyflow.v2.vault.detect.DeidentifyFileRequest;
import com.skyflow.v2.vault.detect.DeidentifyFileResponse;
import com.skyflow.v2.vault.detect.DeidentifyTextRequest;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class DetectController extends VaultClient {
    private static final Gson gson = new GsonBuilder().serializeNulls().create();

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    public DeidentifyTextResponse deidentifyText(DeidentifyTextRequest deidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_TRIGGERED.getLog());
        DeidentifyStringResponse deidentifyStringResponse = null;
        DeidentifyTextResponse deidentifyTextResponse = null;
        try {
            // Validate the request
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DEIDENTIFY_TEXT_REQUEST.getLog());
            Validations.validateDeidentifyTextRequest(deidentifyTextRequest);
            setBearerToken();

            // Parse the request to DeidentifyStringRequest
            String vaultId = super.getVaultConfig().getVaultId();
            DeidentifyStringRequest request = getDeidentifyStringRequest(deidentifyTextRequest, vaultId);

            // Call the API to de-identify the string
            deidentifyStringResponse = super.getDetectTextApi().deidentifyString(request);

            // Parse the response to DeIdentifyTextResponse
            deidentifyTextResponse = getDeIdentifyTextResponse(deidentifyStringResponse);
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            String bodyString = gson.toJson(ex.body());
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw new SkyflowException(ex.statusCode(), ex, ex.headers(), bodyString);
        }
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_SUCCESS.getLog());
        return deidentifyTextResponse;
    }

    public ReidentifyTextResponse reidentifyText(ReidentifyTextRequest reidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_TRIGGERED.getLog());
        ReidentifyTextResponse reidentifyTextResponse = null;
        try {
            // Validate the request
            LogUtil.printInfoLog(InfoLogs.VALIDATE_REIDENTIFY_TEXT_REQUEST.getLog());
            Validations.validateReidentifyTextRequest(reidentifyTextRequest);
            setBearerToken();
            // Parse the request to ReidentifyTextRequest
            String vaultId = super.getVaultConfig().getVaultId();
            ReidentifyStringRequest request = getReidentifyStringRequest(reidentifyTextRequest, vaultId);

            // Call the API to re-identify the string
            ReidentifyStringResponse reidentifyStringResponse = super.getDetectTextApi().reidentifyString(request);

            // Parse the response to ReidentifyTextResponse
            reidentifyTextResponse = new ReidentifyTextResponse(reidentifyStringResponse.getText().orElse(null));
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            String bodyString = gson.toJson(ex.body());
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw new SkyflowException(ex.statusCode(), ex, ex.headers(), bodyString);
        }
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_SUCCESS.getLog());
        return reidentifyTextResponse;
    }

    public DeidentifyFileResponse deidentifyFile(DeidentifyFileRequest request) throws SkyflowException {
        DeidentifyFileResponse response;
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_FILE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DEIDENTIFY_FILE_REQUEST.getLog());
            Validations.validateDeidentifyFileRequest(request);
            setBearerToken();

            String vaultId = super.getVaultConfig().getVaultId();

            File file;
            if (request.getFileInput().getFilePath() != null) {
                file = new File(request.getFileInput().getFilePath());
            } else {
                file = request.getFileInput().getFile();
            }
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);
            String base64Content;

            try {
                base64Content = encodeFileToBase64(file);
            } catch (IOException ioe) {
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.FailedToEncodeFile.getMessage());
            }


            com.skyflow.v2.generated.rest.types.DeidentifyFileResponse apiResponse = processFileByType(fileExtension, base64Content, request, vaultId);
            try {
                response = pollForResults(apiResponse.getRunId(), request.getWaitTime());
            } catch (Exception ex) {
                throw new SkyflowException(ErrorCode.SERVER_ERROR.getCode(), ErrorMessage.PollingForResultsFailed.getMessage());
            }

            if (DeidentifyFileStatus.SUCCESS.value().equalsIgnoreCase(response.getStatus())) {
                String base64File = response.getFileBase64();
                if (base64File != null) {
                    byte[] decodedBytes = Base64.getDecoder().decode(base64File);
                    String outputDir = request.getOutputDirectory();
                    String outputFileName = Constants.PROCESSED_FILE_NAME_PREFIX + fileName;
                    File outputFile;
                    if (outputDir != null && !outputDir.isEmpty()) {
                        outputFile = new File(outputDir, outputFileName);
                    } else {
                        outputFile = new File(outputFileName);
                    }
                    try {
                        java.nio.file.Files.write(outputFile.toPath(), decodedBytes);
                    } catch (IOException ioe) {
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.FailedtoSaveProcessedFile.getMessage());
                    }

                }
            }
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
        return response;
    }

    private String getFileExtension(String fileName) {
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String encodeFileToBase64(File file) throws IOException {
        byte[] fileContent = Files.readAllBytes(file.toPath());
        return Base64.getEncoder().encodeToString(fileContent);
    }

    private DeidentifyFileResponse pollForResults(String runId, Integer maxWaitTime) throws Exception {
        int currentWaitTime = 1;
        maxWaitTime = maxWaitTime == null ? 64 : maxWaitTime;

        DeidentifyStatusResponse response = null;

        while (true) {
            try {
                GetRunRequest getRunRequest = GetRunRequest.builder()
                        .vaultId(super.getVaultConfig().getVaultId())
                        .build();
                response = super.getDetectFileAPi()
                        .getRun(runId, getRunRequest);

                DeidentifyStatusResponseStatus status = response.getStatus();

                if (DeidentifyFileStatus.IN_PROGRESS.value().equalsIgnoreCase(String.valueOf(status))) {
                    if (currentWaitTime >= maxWaitTime) {
                        return new DeidentifyFileResponse(runId, DeidentifyFileStatus.IN_PROGRESS.value());
                    }

                    int nextWaitTime = currentWaitTime * 2;
                    int waitTime;

                    if (nextWaitTime >= maxWaitTime) {
                        waitTime = maxWaitTime - currentWaitTime;
                        currentWaitTime = maxWaitTime;
                    } else {
                        waitTime = nextWaitTime;
                        currentWaitTime = nextWaitTime;
                    }

                    Thread.sleep(waitTime * 1000);

                } else if (status == DeidentifyStatusResponseStatus.SUCCESS ||
                        status == DeidentifyStatusResponseStatus.FAILED) {
                    return parseDeidentifyFileResponse(response, runId, status.toString());
                }
            } catch (ApiClientApiException e) {
                String bodyString = gson.toJson(e.body());
                LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
                throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
            }
        }

    }

    private static synchronized DeidentifyFileResponse parseDeidentifyFileResponse(DeidentifyStatusResponse response,
                                                                                   String runId, String status) throws SkyflowException {
        DeidentifyFileOutput firstOutput = getFirstOutput(response);

        Object wordCharObj = response.getAdditionalProperties().get("word_character_count");
        Integer wordCount = null;
        Integer charCount = null;

        if (wordCharObj instanceof Map) {
            Map<?, ?> wordCharMap = (Map<?, ?>) wordCharObj;
            Object wc = wordCharMap.get("word_count");
            Object cc = wordCharMap.get("character_count");
            if (wc instanceof Number) {
                wordCount = ((Number) wc).intValue();
            }
            if (cc instanceof Number) {
                charCount = ((Number) cc).intValue();
            }
        }

        File processedFileObject = null;
        FileInfo fileInfo = null;
        Optional<String> processedFileBase64 = firstOutput != null ? firstOutput.getProcessedFile() : Optional.empty();
        Optional<String> processedFileExtension = firstOutput != null ? firstOutput.getProcessedFileExtension() : Optional.empty();

        if (processedFileBase64.isPresent() && processedFileExtension.isPresent()) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(processedFileBase64.get());
                String suffix = "." + processedFileExtension.get();
                String fileName = Constants.DEIDENTIFIED_FILE_PREFIX + suffix;
                processedFileObject = new File(System.getProperty("java.io.tmpdir"), fileName);
                Files.write(processedFileObject.toPath(), decodedBytes);
                 fileInfo = new FileInfo(processedFileObject);
                processedFileObject.deleteOnExit();
            } catch (IOException ioe) {
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.FailedToEncodeFile.getMessage());
            }
        }

        return new DeidentifyFileResponse(
                fileInfo,
                firstOutput.getProcessedFile().orElse(null),
                firstOutput.getProcessedFileType().get().toString(),
                firstOutput.getProcessedFileExtension().get(),
                wordCount,
                charCount,
                response.getSize().map(Double::valueOf).orElse(null),
                response.getDuration().map(Double::valueOf).orElse(null),
                response.getPages().orElse(null),
                response.getSlides().orElse(null),
                getEntities(response),
                runId,
                status,
                null
        );
    }

    private static synchronized DeidentifyFileOutput getFirstOutput(DeidentifyStatusResponse response) {
        List<DeidentifyFileOutput> outputs = response.getOutput();
        return outputs != null && !outputs.isEmpty() ? outputs.get(0) : null;
    }

    private static synchronized List<FileEntityInfo> getEntities(DeidentifyStatusResponse response) {
        List<FileEntityInfo> entities = new ArrayList<>();

        List<DeidentifyFileOutput> outputs = response.getOutput();
        DeidentifyFileOutput deidentifyFileOutput = outputs != null && !outputs.isEmpty() ? outputs.get(1) : null;

        if (deidentifyFileOutput != null) {
            entities.add(new FileEntityInfo(
                    deidentifyFileOutput.getProcessedFile().orElse(null),
                    deidentifyFileOutput.getProcessedFileType().orElse(null),
                    deidentifyFileOutput.getProcessedFileExtension().orElse(null)
            ));
        }

        return entities;
    }

    private com.skyflow.v2.generated.rest.types.DeidentifyFileResponse processFileByType(String fileExtension, String base64Content,                                                                         DeidentifyFileRequest request, String vaultId) throws SkyflowException {
        switch (fileExtension.toLowerCase()) {
            case "txt":
                com.skyflow.v2.generated.rest.resources.files.requests.DeidentifyTextRequest textFileRequest =
                        super.getDeidentifyTextFileRequest(request, vaultId, base64Content);
                return super.getDetectFileAPi().deidentifyText(textFileRequest);

            case "mp3":
            case "wav":
                DeidentifyAudioRequest audioRequest =
                        super.getDeidentifyAudioRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyAudio(audioRequest);

            case "pdf":
                DeidentifyPdfRequest pdfRequest =
                        super.getDeidentifyPdfRequest(request, vaultId, base64Content);

                return super.getDetectFileAPi().deidentifyPdf(pdfRequest);

            case "jpg":
            case "jpeg":
            case "png":
            case "bmp":
            case "tif":
            case "tiff":
                DeidentifyImageRequest imageRequest =
                        super.getDeidentifyImageRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyImage(imageRequest);

            case "ppt":
            case "pptx":
                DeidentifyPresentationRequest presentationRequest =
                        super.getDeidentifyPresentationRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyPresentation(presentationRequest);

            case "csv":
            case "xls":
            case "xlsx":
                DeidentifySpreadsheetRequest spreadsheetRequest =
                        super.getDeidentifySpreadsheetRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifySpreadsheet(spreadsheetRequest);

            case "doc":
            case "docx":
                DeidentifyDocumentRequest documentRequest =
                        super.getDeidentifyDocumentRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyDocument(documentRequest);

            case "json":
            case "xml":
                DeidentifyStructuredTextRequest structuredTextRequest =
                        super.getDeidentifyStructuredTextRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyStructuredText(structuredTextRequest);

            default:
                com.skyflow.v2.generated.rest.resources.files.requests.DeidentifyFileRequest genericFileRequest =
                        super.getDeidentifyGenericFileRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyFile(genericFileRequest);
        }
    }

    public DeidentifyFileResponse getDetectRun(GetDetectRunRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.GET_DETECT_RUN_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_GET_DETECT_RUN_REQUEST.getLog());
            Validations.validateGetDetectRunRequest(request);
            setBearerToken();
            String runId = request.getRunId();
            String vaultId = super.getVaultConfig().getVaultId();

            GetRunRequest getRunRequest =
                    GetRunRequest.builder()
                            .vaultId(vaultId)
                            .build();

            com.skyflow.v2.generated.rest.types.DeidentifyStatusResponse apiResponse =
                    super.getDetectFileAPi().getRun(runId, getRunRequest);

            return parseDeidentifyFileResponse(apiResponse, runId, apiResponse.getStatus().toString());
        } catch (ApiClientApiException e) {
            String bodyString = gson.toJson(e.body());
            LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }
}
