package com.skyflow.vault.controller;

import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.resources.files.requests.*;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.detect.*;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyFileResponse;
import com.skyflow.vault.detect.DeidentifyTextRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;

public final class DetectController extends VaultClient {

    public DetectController(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
    }

    public DeidentifyTextResponse deidentifyText(DeidentifyTextRequest deidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_TRIGGERED.getLog());
        DeidentifyStringResponse deidentifyStringResponse = null;
        DeidentifyTextResponse deidentifyTextResponse = null;
        try {
            // Validate the request
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
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw ex;
        }
        return deidentifyTextResponse;
    }

    public ReidentifyTextResponse reidentifyText(ReidentifyTextRequest reidentifyTextRequest) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_TRIGGERED.getLog());
        ReidentifyTextResponse reidentifyTextResponse = null;
        try {
            // Validate the request
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
            LogUtil.printErrorLog(ErrorLogs.REIDENTIFY_TEXT_REQUEST_REJECTED.getLog());
            throw ex;
        }
        return reidentifyTextResponse;
    }

    public DeidentifyFileResponse deidentifyFile(DeidentifyFileRequest request) throws SkyflowException {
        LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_FILE_TRIGGERED.getLog());
        try {
            LogUtil.printInfoLog(InfoLogs.VALIDATE_DEIDENTIFY_FILE_REQUEST.getLog());
            Validations.validateDeidentifyFileRequest(request);
            setBearerToken();

            String vaultId = super.getVaultConfig().getVaultId();

            File file = request.getFile();
            String fileName = file.getName();
            String fileExtension = getFileExtension(fileName);
            String base64Content = encodeFileToBase64(file);


            com.skyflow.generated.rest.types.DeidentifyFileResponse apiResponse = processFileByType(fileExtension, base64Content, request, vaultId);
            DeidentifyFileResponse response = pollForResults(apiResponse.getRunId(), request.getWaitTime());

            if ("SUCCESS".equalsIgnoreCase(response.getStatus())) {
                String base64File = response.getFile();
                if (base64File != null) {
                    byte[] decodedBytes = Base64.getDecoder().decode(base64File);
                    String outputDir = request.getOutputDirectory();
                    String outputFileName = "processed-" + fileName;
                    File outputFile;
                    if (outputDir != null && !outputDir.isEmpty()) {
                        outputFile = new File(outputDir, outputFileName);
                    } else {
                        outputFile = new File(outputFileName);
                    }
                    java.nio.file.Files.write(outputFile.toPath(), decodedBytes);

                }
            }
            return response;
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.DEIDENTIFY_FILE_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), e.body().toString());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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

                if (Objects.equals(status.toString(), "IN_PROGRESS")) {
                    if (currentWaitTime >= maxWaitTime) {
                        return new DeidentifyFileResponse(
                                null,  // file
                                null,  // type
                                null,  // extension
                                null,  // wordCount
                                null,  // charCount
                                null,  // sizeInKb
                                null,  // durationInSeconds
                                null,  // pageCount
                                null,  // slideCount
                                null,  // entities
                                runId, // runId
                                "IN_PROGRESS", // status
                                null   // errors
                        );
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
                    return parseDeidentifyFileResponse(response, runId, status.toString().toLowerCase());
                }
            } catch (ApiClientApiException e) {
                LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
                throw new SkyflowException(e.statusCode(), e, e.headers(), e.body().toString());
            }
        }

    }


    private static synchronized DeidentifyFileResponse parseDeidentifyFileResponse(DeidentifyStatusResponse response,
                                                               String runId, String status) {
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


        return new DeidentifyFileResponse(
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


    private com.skyflow.generated.rest.types.DeidentifyFileResponse processFileByType(String fileExtension, String base64Content,
                                                                                      DeidentifyFileRequest request, String vaultId) {
        switch(fileExtension.toLowerCase()) {
            case "txt":
                com.skyflow.generated.rest.resources.files.requests.DeidentifyTextRequest textFileRequest =
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

                com.skyflow.generated.rest.types.DeidentifyFileResponse result2 = super.getDetectFileAPi().deidentifyPdf(pdfRequest);
                return result2;

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
                textFileRequest = super.getDeidentifyTextFileRequest(request, vaultId, base64Content);
                return super.getDetectFileAPi().deidentifyText(textFileRequest);
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

            com.skyflow.generated.rest.types.DeidentifyStatusResponse apiResponse =
                    super.getDetectFileAPi().getRun(runId, getRunRequest);

            return parseDeidentifyFileResponse(apiResponse, runId, apiResponse.getStatus().toString().toLowerCase());
        } catch (ApiClientApiException e) {
            LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), e.body().toString());
        }
    }
}