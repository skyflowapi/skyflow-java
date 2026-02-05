package com.skyflow.vault.controller;

import com.google.gson.*;
import com.skyflow.VaultClient;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DeidentifyFileStatus;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.core.ApiClientApiException;
import com.skyflow.generated.rest.core.RequestOptions;
import com.skyflow.generated.rest.resources.files.requests.*;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.logs.InfoLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
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
    Gson gson = new GsonBuilder()
            .registerTypeAdapter(Optional.class, (JsonSerializer<Optional<?>>) (src, typeOfSrc, context) ->
                    src.map(context::serialize).orElse(null))
            .serializeNulls()
            .create();
    private static final JsonObject SKY_METADATA = Utils.getMetrics();

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

            // get SDK metrics and call the API to de-identify the string
            RequestOptions requestOptions = RequestOptions.builder().addHeader(Constants.SDK_METRICS_HEADER_KEY, SKY_METADATA.toString()).build();
            deidentifyStringResponse = super.getDetectTextApi().deidentifyString(request, requestOptions);

            // Parse the response to DeIdentifyTextResponse
            deidentifyTextResponse = getDeIdentifyTextResponse(deidentifyStringResponse);
            LogUtil.printInfoLog(InfoLogs.DEIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            String bodyString = extractBodyAsString(ex);
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

            // Get SDK metrics and call the API to re-identify the string
            RequestOptions requestOptions = RequestOptions.builder().addHeader(Constants.SDK_METRICS_HEADER_KEY, SKY_METADATA.toString()).build();
            IdentifyResponse reidentifyStringResponse = super.getDetectTextApi().reidentifyString(request, requestOptions);

            // Parse the response to ReidentifyTextResponse
            reidentifyTextResponse = new ReidentifyTextResponse(reidentifyStringResponse.getText());
            LogUtil.printInfoLog(InfoLogs.REIDENTIFY_TEXT_REQUEST_RESOLVED.getLog());
        } catch (ApiClientApiException ex) {
            String bodyString = extractBodyAsString(ex);
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


            com.skyflow.generated.rest.types.DeidentifyFileResponse apiResponse = processFileByType(fileExtension, base64Content, request, vaultId);
            try {
                response = pollForResults(apiResponse.getRunId().get(), request.getWaitTime());
            } catch (Exception ex) {
                throw new SkyflowException(ErrorCode.SERVER_ERROR.getCode(), ErrorMessage.PollingForResultsFailed.getMessage());
            }

            if (DeidentifyFileStatus.SUCCESS.value().equalsIgnoreCase(response.getStatus())) {
                String base64File = response.getFileBase64();
                response.getEntities().get(0).getFile();
                if (base64File != null) {
                    byte[] decodedBytes = Base64.getDecoder().decode(base64File);
                    String outputDir = request.getOutputDirectory();
                    String outputFileName = Constants.PROCESSED_FILE_NAME_PREFIX + fileName.substring(0, fileName.lastIndexOf('.')) + "." + response.getExtension();
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

                List<FileEntityInfo> entities = response.getEntities();
                if (entities != null && !entities.isEmpty()) {
                    FileEntityInfo entityInfo = entities.get(0);
                    String entityBase64 = entityInfo.getFile();
                    String outputDir = request.getOutputDirectory();
                    if (entityBase64 != null) {
                        byte[] entityDecodedBytes = Base64.getDecoder().decode(entityBase64);
                        String entityFileName = Constants.PROCESSED_FILE_NAME_PREFIX + fileName.substring(0, fileName.lastIndexOf('.')) + Constants.FileExtension.JSON;
                        File entityFile;
                        if (outputDir != null && !outputDir.isEmpty()) {
                            entityFile = new File(outputDir, entityFileName);
                        } else {
                            entityFile = new File(entityFileName);
                        }
                        try {
                            java.nio.file.Files.write(entityFile.toPath(), entityDecodedBytes);
                        } catch (IOException ioe) {
                            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.FailedtoSaveProcessedFile.getMessage());
                        }
                    }
                }
            }
        } catch (ApiClientApiException e) {
            String bodyString = extractBodyAsString(e);
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

        DetectRunsResponse response = null;

        while (true) {
            try {
                GetRunRequest getRunRequest = GetRunRequest.builder()
                        .vaultId(super.getVaultConfig().getVaultId())
                        .build();

                RequestOptions requestOptions = RequestOptions.builder().addHeader(Constants.SDK_METRICS_HEADER_KEY, SKY_METADATA.toString()).build();
                response = super.getDetectFileAPi()
                        .getRun(runId, getRunRequest, requestOptions);

                DetectRunsResponseStatus status = response.getStatus().get();

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

                } else if (status == DetectRunsResponseStatus.SUCCESS ||
                        status == DetectRunsResponseStatus.FAILED) {
                    return parseDeidentifyFileResponse(response, runId, status.toString());
                }
            } catch (ApiClientApiException e) {
                String bodyString = gson.toJson(e.body());
                LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
                throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
            }
        }

    }

    private static synchronized DeidentifyFileResponse parseDeidentifyFileResponse(DetectRunsResponse response,
                                                                                   String runId, String status) throws SkyflowException {

        DeidentifiedFileOutput firstOutput = getFirstOutput(response);

        if (firstOutput == null) {
            return new DeidentifyFileResponse(
                    null,
                    null,
                    response.getOutputType().get().toString(),
                    null,
                    null,
                    null,
                    response.getSize().get(),
                    response.getDuration().get(),
                    response.getPages().orElse(null),
                    response.getSlides().orElse(null),
                    getEntities(response),
                    runId,
                    response.getStatus().get().name()
            );
        }

        Integer wordCount = null;
        Integer charCount = null;

        WordCharacterCount wordCharacterCount = response.getWordCharacterCount().orElse(null);
        if (wordCharacterCount != null) {
            wordCount = wordCharacterCount.getWordCount().orElse(null);
            charCount = wordCharacterCount.getCharacterCount().orElse(null);
        }

        File processedFileObject = null;
        FileInfo fileInfo = null;
        Optional<String> processedFileBase64 = Optional.of(firstOutput).flatMap(DeidentifiedFileOutput::getProcessedFile);
        Optional<DeidentifiedFileOutputProcessedFileExtension>
                processedFileExtension = Optional.of(firstOutput).flatMap(DeidentifiedFileOutput::getProcessedFileExtension);
        if (processedFileBase64.isPresent() && processedFileExtension.isPresent()) {
            try {
                byte[] decodedBytes = Base64.getDecoder().decode(processedFileBase64.get());
                String suffix = "." + processedFileExtension.get();
                String fileName = Constants.DEIDENTIFIED_FILE_PREFIX + suffix;
                processedFileObject = new File(System.getProperty(Constants.SystemProperty.JAVA_TEMP_DIR), fileName);
                Files.write(processedFileObject.toPath(), decodedBytes);
                 fileInfo = new FileInfo(processedFileObject);
                processedFileObject.deleteOnExit();
            } catch (IOException ioe) {
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.FailedToEncodeFile.getMessage());
            }
        }

        String processedFileType = firstOutput.getProcessedFileType()
                .map(Object::toString)
                .orElse(DetectRunsResponseStatus.UNKNOWN.toString());

        String fileExtension = firstOutput.getProcessedFileExtension().get().toString();
        Float sizeInKb = response.getSize().orElse(null);
        Float durationInSeconds = response.getDuration().orElse(null);
        DeidentifyFileResponse deidentifyFileResponse = new DeidentifyFileResponse(
                fileInfo,
                firstOutput.getProcessedFile().orElse(null),
                processedFileType,
                fileExtension,
                wordCount,
                charCount,
                sizeInKb,
                durationInSeconds,
                response.getPages().orElse(null),
                response.getSlides().orElse(null),
                getEntities(response),
                runId,
                status
        );

        return deidentifyFileResponse;
    }

    private static synchronized DeidentifiedFileOutput getFirstOutput(DetectRunsResponse response) {
        List<DeidentifiedFileOutput> outputs = response.getOutput().get();
        return outputs != null && !outputs.isEmpty() ? outputs.get(0) : null;
    }

    private static synchronized List<FileEntityInfo> getEntities(DetectRunsResponse response) {
        List<FileEntityInfo> entities = new ArrayList<>();

        Optional<List<DeidentifiedFileOutput>> outputs = response.getOutput();
        DeidentifiedFileOutput deidentifyFileOutput = outputs.isPresent() ? outputs.get().get(0) : null;

        if (deidentifyFileOutput != null) {
            entities.add(new FileEntityInfo(
                    deidentifyFileOutput.getProcessedFile().orElse(null),
                    deidentifyFileOutput.getProcessedFileType().orElse(null),
                    deidentifyFileOutput.getProcessedFileExtension().orElse(null)
            ));
        }

        return entities;
    }

    private String extractBodyAsString(ApiClientApiException e) {
        return e.statusCode() == 500
                ? e.body().toString()
                : gson.toJson(e.body());
    }


    private com.skyflow.generated.rest.types.DeidentifyFileResponse processFileByType(String fileExtension, String base64Content, DeidentifyFileRequest request, String vaultId) throws SkyflowException {
        switch (fileExtension.toLowerCase()) {
            case Constants.FileFormatType.TXT:
                com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText textFileRequest =
                        super.getDeidentifyTextFileRequest(request, vaultId, base64Content);
                return super.getDetectFileAPi().deidentifyText(textFileRequest);

            case Constants.FileFormatType.MP3:
            case Constants.FileFormatType.WAV:
                DeidentifyFileAudioRequestDeidentifyAudio audioRequest =
                        super.getDeidentifyAudioRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyAudio(audioRequest);

            case Constants.FileFormatType.PDF:
                DeidentifyFileDocumentPdfRequestDeidentifyPdf pdfRequest =
                        super.getDeidentifyPdfRequest(request, vaultId, base64Content);

                return super.getDetectFileAPi().deidentifyPdf(pdfRequest);

            case Constants.FileFormatType.JPG:
            case Constants.FileFormatType.JPEG:
            case Constants.FileFormatType.PNG:
            case Constants.FileFormatType.BMP:
            case Constants.FileFormatType.TIF:
            case Constants.FileFormatType.TIFF:
                DeidentifyFileImageRequestDeidentifyImage imageRequest =
                        super.getDeidentifyImageRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyImage(imageRequest);

            case Constants.FileFormatType.PPT:
            case Constants.FileFormatType.PPTX:
                DeidentifyFileRequestDeidentifyPresentation presentationRequest =
                        super.getDeidentifyPresentationRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyPresentation(presentationRequest);

            case Constants.FileFormatType.CSV:
            case Constants.FileFormatType.XLS:
            case Constants.FileFormatType.XLSX:
                DeidentifyFileRequestDeidentifySpreadsheet spreadsheetRequest =
                        super.getDeidentifySpreadsheetRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifySpreadsheet(spreadsheetRequest);

            case Constants.FileFormatType.DOC:
            case Constants.FileFormatType.DOCX:
                DeidentifyFileRequestDeidentifyDocument documentRequest =
                        super.getDeidentifyDocumentRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyDocument(documentRequest);

            case Constants.FileFormatType.JSON:
            case Constants.FileFormatType.XML:
                DeidentifyFileRequestDeidentifyStructuredText structuredTextRequest =
                        super.getDeidentifyStructuredTextRequest(request, vaultId, base64Content, fileExtension);
                return super.getDetectFileAPi().deidentifyStructuredText(structuredTextRequest);

            default:
                com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequest genericFileRequest =
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

            com.skyflow.generated.rest.types.DetectRunsResponse apiResponse =
                    super.getDetectFileAPi().getRun(runId, getRunRequest);

            return parseDeidentifyFileResponse(apiResponse, runId, apiResponse.getStatus().toString());
        } catch (ApiClientApiException e) {
            String bodyString = extractBodyAsString(e);
            LogUtil.printErrorLog(ErrorLogs.GET_DETECT_RUN_REQUEST_REJECTED.getLog());
            throw new SkyflowException(e.statusCode(), e, e.headers(), bodyString);
        }
    }
}
