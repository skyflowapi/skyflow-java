package com.skyflow;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.DetectEntities;
import com.skyflow.enums.DetectOutputTranscriptions;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileAudioRequestDeidentifyAudio;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileDocumentPdfRequestDeidentifyPdf;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileImageRequestDeidentifyImage;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyDocument;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyPresentation;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifySpreadsheet;
import com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyStructuredText;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileAudioRequestDeidentifyAudioOutputTranscription;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileImageRequestDeidentifyImageEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileImageRequestDeidentifyImageMaskingMethod;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestDeidentifyDocumentEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestDeidentifyPresentationEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestDeidentifySpreadsheetEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestDeidentifyStructuredTextEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestDeidentifyTextEntityTypesItem;
import com.skyflow.generated.rest.resources.files.types.DeidentifyFileRequestEntityTypesItem;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.strings.StringsClient;
import com.skyflow.generated.rest.resources.strings.requests.DeidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.requests.ReidentifyStringRequest;
import com.skyflow.generated.rest.resources.strings.types.DeidentifyStringRequestEntityTypesItem;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.BatchRecordMethod;
import com.skyflow.generated.rest.types.DeidentifyStringResponse;
import com.skyflow.generated.rest.types.FileData;
import com.skyflow.generated.rest.types.FileDataDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyAudio;
import com.skyflow.generated.rest.types.FileDataDeidentifyAudioDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyDocument;
import com.skyflow.generated.rest.types.FileDataDeidentifyDocumentDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyImage;
import com.skyflow.generated.rest.types.FileDataDeidentifyImageDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyPdf;
import com.skyflow.generated.rest.types.FileDataDeidentifyPresentation;
import com.skyflow.generated.rest.types.FileDataDeidentifyPresentationDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifySpreadsheet;
import com.skyflow.generated.rest.types.FileDataDeidentifySpreadsheetDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyStructuredText;
import com.skyflow.generated.rest.types.FileDataDeidentifyStructuredTextDataFormat;
import com.skyflow.generated.rest.types.FileDataDeidentifyText;
import com.skyflow.generated.rest.types.Format;
import com.skyflow.generated.rest.types.FormatMaskedItem;
import com.skyflow.generated.rest.types.FormatPlaintextItem;
import com.skyflow.generated.rest.types.FormatRedactedItem;
import com.skyflow.generated.rest.types.ShiftDates;
import com.skyflow.generated.rest.types.ShiftDatesEntityTypesItem;
import com.skyflow.generated.rest.types.StringResponseEntities;
import com.skyflow.generated.rest.types.TokenTypeMapping;
import com.skyflow.generated.rest.types.TokenTypeMappingEntityOnlyItem;
import com.skyflow.generated.rest.types.TokenTypeMappingEntityUnqCounterItem;
import com.skyflow.generated.rest.types.TokenTypeMappingVaultTokenItem;
import com.skyflow.generated.rest.types.Transformations;
import com.skyflow.generated.rest.types.V1BatchRecord;
import com.skyflow.generated.rest.types.V1DetokenizeRecordRequest;
import com.skyflow.generated.rest.types.V1FieldRecords;
import com.skyflow.generated.rest.types.V1TokenizeRecordRequest;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.detect.AudioBleep;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.detect.DeidentifyTextResponse;
import com.skyflow.vault.detect.EntityInfo;
import com.skyflow.vault.detect.ReidentifyTextRequest;
import com.skyflow.vault.detect.TextIndex;
import com.skyflow.vault.detect.TokenFormat;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;

import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.Request;


public class VaultClient {
    private final VaultConfig vaultConfig;
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    private OkHttpClient sharedHttpClient;
    private String currentVaultURL;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super();
        this.vaultConfig = vaultConfig;
        this.commonCredentials = credentials;
        this.apiClientBuilder = new ApiClientBuilder();
        this.apiClient = null;
        updateVaultURL();
    }

    protected RecordsClient getRecordsApi() {
        return this.apiClient.records();
    }

    protected TokensClient getTokensApi() {
        return this.apiClient.tokens();
    }

    protected StringsClient getDetectTextApi() {
        return this.apiClient.strings();
    }

    protected FilesClient getDetectFileAPi() {
        return this.apiClient.files();
    }

    protected QueryClient getQueryApi() {
        return this.apiClient.query();
    }

    protected VaultConfig getVaultConfig() {
        return vaultConfig;
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        prioritiseCredentials();
    }

    protected void updateVaultConfig() throws SkyflowException {
        updateVaultURL();
        prioritiseCredentials();
    }

    protected V1DetokenizePayload getDetokenizePayload(DetokenizeRequest request) {
        List<V1DetokenizeRecordRequest> recordRequests = new ArrayList<>();

        for (DetokenizeData detokenizeDataRecord : request.getDetokenizeData()) {
            V1DetokenizeRecordRequest recordRequest = V1DetokenizeRecordRequest.builder()
                    .token(detokenizeDataRecord.getToken())
                    .redaction(detokenizeDataRecord.getRedactionType().getRedaction())
                    .build();
            recordRequests.add(recordRequest);
        }

        return V1DetokenizePayload.builder()
                .continueOnError(request.getContinueOnError())
                .downloadUrl(request.getDownloadUrl())
                .detokenizationParameters(recordRequests)
                .build();
    }

    protected RecordServiceInsertRecordBody getBulkInsertRequestBody(InsertRequest request) {
        List<HashMap<String, Object>> values = request.getValues();
        List<HashMap<String, Object>> tokens = request.getTokens();
        List<V1FieldRecords> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            V1FieldRecords.Builder recordBuilder = V1FieldRecords.builder().fields(values.get(index));
            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }
            records.add(recordBuilder.build());
        }

        return RecordServiceInsertRecordBody.builder()
                .tokenization(request.getReturnTokens())
                .homogeneous(request.getHomogeneous())
                .upsert(request.getUpsert())
                .byot(request.getTokenMode().getByot())
                .records(records)
                .build();
    }

    protected RecordServiceBatchOperationBody getBatchInsertRequestBody(InsertRequest request) {
        ArrayList<HashMap<String, Object>> values = request.getValues();
        ArrayList<HashMap<String, Object>> tokens = request.getTokens();
        List<V1BatchRecord> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            V1BatchRecord.Builder recordBuilder = V1BatchRecord.builder()
                    .method(BatchRecordMethod.POST)
                    .tableName(request.getTable())
                    .upsert(request.getUpsert())
                    .tokenization(request.getReturnTokens())
                    .fields(values.get(index));

            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }

            records.add(recordBuilder.build());
        }

        return RecordServiceBatchOperationBody.builder()
                .continueOnError(true)
                .byot(request.getTokenMode().getByot())
                .records(records)
                .build();
    }

    protected RecordServiceUpdateRecordBody getUpdateRequestBody(UpdateRequest request) {
        RecordServiceUpdateRecordBody.Builder updateRequestBodyBuilder = RecordServiceUpdateRecordBody.builder();
        updateRequestBodyBuilder.byot(request.getTokenMode().getByot());
        updateRequestBodyBuilder.tokenization(request.getReturnTokens());
        V1FieldRecords.Builder recordBuilder = V1FieldRecords.builder();
        HashMap<String, Object> values = request.getData();

        if (values != null) {
            recordBuilder.fields(values);
        }

        HashMap<String, Object> tokens = request.getTokens();
        if (tokens != null) {
            recordBuilder.tokens(tokens);
        }

        updateRequestBodyBuilder.record(recordBuilder.build());

        return updateRequestBodyBuilder.build();
    }

    protected V1TokenizePayload getTokenizePayload(TokenizeRequest request) {
        List<V1TokenizeRecordRequest> tokenizationParameters = new ArrayList<>();

        for (ColumnValue columnValue : request.getColumnValues()) {
            V1TokenizeRecordRequest.Builder recordBuilder = V1TokenizeRecordRequest.builder();
            String value = columnValue.getValue();
            recordBuilder.value(value);
            String columnGroup = columnValue.getColumnGroup();
            recordBuilder.columnGroup(columnGroup);
            tokenizationParameters.add(recordBuilder.build());
        }

        V1TokenizePayload.Builder payloadBuilder = V1TokenizePayload.builder();

        if (!tokenizationParameters.isEmpty()) {
            payloadBuilder.tokenizationParameters(tokenizationParameters);
        }

        return payloadBuilder.build();
    }

    protected File getFileForFileUpload(FileUploadRequest fileUploadRequest) throws IOException {
        if (fileUploadRequest.getFilePath() != null) {
            return new File(fileUploadRequest.getFilePath());
        } else if (fileUploadRequest.getBase64() != null) {
            byte[] decodedBytes = Base64.getDecoder().decode(fileUploadRequest.getBase64());
            File file = new File(fileUploadRequest.getFileName());
            Files.write(file.toPath(), decodedBytes);
            return file;
        } else if (fileUploadRequest.getFileObject() != null) {
            return fileUploadRequest.getFileObject();
        }
        return null;
    }

    protected synchronized void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (Token.isExpired(token)) {
            // Token.isExpired(null/empty) returns true, so this branch also covers first-time generation.
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
        if (apiClient == null) {
            updateExecutorInHTTP();
            this.apiClient = this.apiClientBuilder.build();
        }
    }

    protected DeidentifyTextResponse getDeIdentifyTextResponse(DeidentifyStringResponse deidentifyStringResponse) {
        Optional<List<StringResponseEntities>> detectedEntities = deidentifyStringResponse.getEntities();

        List<EntityInfo> entities = new ArrayList<>();

        if (detectedEntities.isPresent()) {
            for (StringResponseEntities e : detectedEntities.get()) {
                entities.add(convertDetectedEntityToEntityInfo(e));
            }
        }

        return new DeidentifyTextResponse(
                deidentifyStringResponse.getProcessedText().get(),
                entities,
                deidentifyStringResponse.getWordCount().get(),
                deidentifyStringResponse.getCharacterCount().get()
        );
    }

    protected DeidentifyStringRequest getDeidentifyStringRequest(DeidentifyTextRequest deIdentifyTextRequest, String vaultId) throws SkyflowException {
        List<DetectEntities> entities = deIdentifyTextRequest.getEntities();

        List<DeidentifyStringRequestEntityTypesItem> mappedEntityTypes = null;
        if (entities != null) {
            mappedEntityTypes = deIdentifyTextRequest.getEntities().stream()
                    .map(detectEntity -> DeidentifyStringRequestEntityTypesItem.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        TokenFormat tokenFormat = deIdentifyTextRequest.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(deIdentifyTextRequest.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(deIdentifyTextRequest.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(deIdentifyTextRequest.getTransformations()));

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .vaultToken(buildTokenEntities(
                        tokenFormat == null ? null : tokenFormat.getVaultToken(), TokenTypeMappingVaultTokenItem.class))
                .entityOnly(buildTokenEntities(
                        tokenFormat == null ? null : tokenFormat.getEntityOnly(), TokenTypeMappingEntityOnlyItem.class))
                .entityUnqCounter(buildTokenEntities(
                        tokenFormat == null ? null : tokenFormat.getEntityUniqueCounter(), TokenTypeMappingEntityUnqCounterItem.class))
                .build();


        return DeidentifyStringRequest.builder()
                .text(deIdentifyTextRequest.getText())
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected ReidentifyStringRequest getReidentifyStringRequest(ReidentifyTextRequest reidentifyTextRequest, String vaultId) throws SkyflowException {
        Optional<List<FormatMaskedItem>> maskEntities = Optional.empty();
        Optional<List<FormatRedactedItem>> redactedEntities = Optional.empty();
        Optional<List<FormatPlaintextItem>> plaintextEntities = Optional.empty();

        if (reidentifyTextRequest.getMaskedEntities() != null) {
            maskEntities = Optional.of(reidentifyTextRequest.getMaskedEntities().stream()
                    .map(detectEntity -> FormatMaskedItem.valueOf(detectEntity.name()))
                    .collect(Collectors.toList()));
        }

        if (reidentifyTextRequest.getPlainTextEntities() != null) {
            plaintextEntities = Optional.of(reidentifyTextRequest.getPlainTextEntities().stream()
                    .map(detectEntity -> FormatPlaintextItem.valueOf(detectEntity.name()))
                    .collect(Collectors.toList()));
        }

        if (reidentifyTextRequest.getRedactedEntities() != null) {
            redactedEntities = Optional.of(reidentifyTextRequest.getRedactedEntities().stream()
                    .map(detectEntity -> FormatRedactedItem.valueOf(detectEntity.name()))
                    .collect(Collectors.toList()));
        }

        Format reidentifyStringRequestFormat = Format.builder()
                .masked(maskEntities)
                .plaintext(plaintextEntities)
                .redacted(redactedEntities)
                .build();


        return ReidentifyStringRequest.builder()
                .text(reidentifyTextRequest.getText())
                .vaultId(vaultId)
                .format(reidentifyStringRequestFormat)
                .build();
    }


    private EntityInfo convertDetectedEntityToEntityInfo(StringResponseEntities detectedEntity) {
        TextIndex textIndex = new TextIndex(
                detectedEntity.getLocation().get().getStartIndex().orElse(0),
                detectedEntity.getLocation().get().getEndIndex().orElse(0)
        );
        TextIndex processedIndex = new TextIndex(
                detectedEntity.getLocation().get().getStartIndexProcessed().orElse(0),
                detectedEntity.getLocation().get().getEndIndexProcessed().orElse(0)
        );

        Map<String, Double> entityScores = detectedEntity.getEntityScores()
                .<Map<String, Double>>map(HashMap::new)
                .orElse(Collections.emptyMap());


        return new EntityInfo(
                detectedEntity.getToken().orElse(""),
                detectedEntity.getValue().orElse(""),
                textIndex,
                processedIndex,
                detectedEntity.getEntityType().orElse(""),
                entityScores);
    }


    private Transformations getTransformations(com.skyflow.vault.detect.Transformations transformations) {
        if (transformations == null || transformations.getShiftDates() == null) {
            return null;
        }

        Optional<List<ShiftDatesEntityTypesItem>> entityTypes = Optional.empty();
        if (!transformations.getShiftDates().getEntities().isEmpty()) {
            entityTypes = Optional.of(transformations.getShiftDates().getEntities().stream()
                    .map(entity -> ShiftDatesEntityTypesItem.valueOf(entity.name()))
                    .collect(Collectors.toList()));
        } else {
            entityTypes = Optional.of(Collections.emptyList());
        }

        return Transformations.builder()
                .shiftDates(ShiftDates.builder()
                        .maxDays(transformations.getShiftDates().getMax())
                        .minDays(transformations.getShiftDates().getMin())
                        .entityTypes(entityTypes)
                        .build())
                .build();
    }

    private <T extends Enum<T>> List<T> getEntityTypes(List<DetectEntities> entities, Class<T> type) {
        if (entities == null) {
            return Collections.emptyList();
        }

        return entities.stream()
                .map(e -> Enum.valueOf(type, e.name()))
                .collect(Collectors.toList());
    }


    protected com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText getDeidentifyTextFileRequest(DeidentifyFileRequest request, String vaultId, String base64Content) {
        List<DeidentifyFileRequestDeidentifyTextEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifyTextEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyText file = FileDataDeidentifyText.builder()
                .base64(base64Content)
                .build();

        return com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }


    protected DeidentifyFileAudioRequestDeidentifyAudio getDeidentifyAudioRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String dataFormat) throws SkyflowException {
        List<DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyAudio deidentifyAudioRequestFile = FileDataDeidentifyAudio.builder().base64(base64Content).dataFormat(mapAudioDataFormat(dataFormat)).build();
        DetectOutputTranscriptions transcription = request.getOutputTranscription();
        Optional<DeidentifyFileAudioRequestDeidentifyAudioOutputTranscription> outputTranscriptionType = Optional.empty();
        if (transcription != null) {
            outputTranscriptionType = Optional.of(DeidentifyFileAudioRequestDeidentifyAudioOutputTranscription.valueOf(transcription.name()));
        }

        AudioBleep bleep = request.getBleep();

        return DeidentifyFileAudioRequestDeidentifyAudio.builder()
                .file(deidentifyAudioRequestFile)
                .vaultId(vaultId)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .entityTypes(mappedEntityTypes)
                .bleepFrequency(bleep != null ? (int) Math.round(bleep.getFrequency()) : null)
                .bleepGain(bleep != null ? (int) Math.round(bleep.getGain()) : null)
                .bleepStartPadding(bleep != null ? (float) Math.round(bleep.getStartPadding()) : null)
                .bleepStopPadding(bleep != null ? (float) Math.round(bleep.getStopPadding()) : null)
                .outputProcessedAudio(request.getOutputProcessedAudio())
                .outputTranscription(outputTranscriptionType)
                .tokenType(tokenType)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyFileDocumentPdfRequestDeidentifyPdf getDeidentifyPdfRequest(DeidentifyFileRequest request, String vaultId, String base64Content) {
        List<DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyPdf file = FileDataDeidentifyPdf.builder()
                .base64(base64Content)
                .build();

        return DeidentifyFileDocumentPdfRequestDeidentifyPdf.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .build();
    }

    protected DeidentifyFileImageRequestDeidentifyImage getDeidentifyImageRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<DeidentifyFileImageRequestDeidentifyImageEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileImageRequestDeidentifyImageEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyImage file = FileDataDeidentifyImage.builder()
                .base64(base64Content)
                .dataFormat(FileDataDeidentifyImageDataFormat.valueOf(format.toUpperCase()))
                .build();

        Optional<DeidentifyFileImageRequestDeidentifyImageMaskingMethod> maskingMethod = Optional.empty();
        if (request.getMaskingMethod() != null) {
            maskingMethod = Optional.of(DeidentifyFileImageRequestDeidentifyImageMaskingMethod.valueOf(request.getMaskingMethod().name()));
        }

        return DeidentifyFileImageRequestDeidentifyImage.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .maskingMethod(maskingMethod)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .outputProcessedImage(request.getOutputProcessedImage())
                .outputOcrText(request.getOutputOcrText())
                .build();
    }

    protected DeidentifyFileRequestDeidentifyPresentation getDeidentifyPresentationRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<DeidentifyFileRequestDeidentifyPresentationEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifyPresentationEntityTypesItem.class);
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyPresentation file = FileDataDeidentifyPresentation.builder()
                .base64(base64Content)
                .dataFormat(FileDataDeidentifyPresentationDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyFileRequestDeidentifyPresentation.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .build();
    }

    protected DeidentifyFileRequestDeidentifySpreadsheet getDeidentifySpreadsheetRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<DeidentifyFileRequestDeidentifySpreadsheetEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifySpreadsheetEntityTypesItem.class);
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifySpreadsheet file = FileDataDeidentifySpreadsheet.builder()
                .base64(base64Content)
                .dataFormat(FileDataDeidentifySpreadsheetDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyFileRequestDeidentifySpreadsheet.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .build();
    }

    protected DeidentifyFileRequestDeidentifyStructuredText getDeidentifyStructuredTextRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<DeidentifyFileRequestDeidentifyStructuredTextEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifyStructuredTextEntityTypesItem.class);
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyStructuredText file = FileDataDeidentifyStructuredText.builder()
                .base64(base64Content)
                .dataFormat(FileDataDeidentifyStructuredTextDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyFileRequestDeidentifyStructuredText.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyFileRequestDeidentifyDocument getDeidentifyDocumentRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<DeidentifyFileRequestDeidentifyDocumentEntityTypesItem> mappedEntityTypes =
                getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifyDocumentEntityTypesItem.class);
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileDataDeidentifyDocument file = FileDataDeidentifyDocument.builder()
                .base64(base64Content)
                .dataFormat(FileDataDeidentifyDocumentDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyFileRequestDeidentifyDocument.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .build();
    }

    protected com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequest getDeidentifyGenericFileRequest(
            DeidentifyFileRequest request, String vaultId, String base64Content, String fileExtension) {

        List<DeidentifyFileRequestEntityTypesItem> mappedEntityTypes =
                getEntityTypes(request.getEntities(), DeidentifyFileRequestEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeMapping tokenType = buildTokenType(tokenFormat);

        FileData file =
                FileData.builder()
                        .base64(base64Content)
                        .dataFormat(fileExtension != null ? FileDataDataFormat.valueOf(fileExtension.toUpperCase()) : null)
                        .build();

        return com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequest.builder()
                .file(file)
                .vaultId(vaultId)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    private TokenTypeMapping buildTokenType(TokenFormat tokenFormat) {
        return TokenTypeMapping.builder()
                .entityOnly(buildTokenEntities(
                        tokenFormat == null ? null : tokenFormat.getEntityOnly(), TokenTypeMappingEntityOnlyItem.class))
                .entityUnqCounter(buildTokenEntities(
                        tokenFormat == null ? null : tokenFormat.getEntityUniqueCounter(), TokenTypeMappingEntityUnqCounterItem.class))
                .build();
    }

    private <T extends Enum<T>> Optional<List<T>> buildTokenEntities(List<DetectEntities> entities, Class<T> type) {
        if (entities == null || entities.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(getEntityTypes(entities, type));
    }


    private FileDataDeidentifyAudioDataFormat mapAudioDataFormat(String dataFormat) throws SkyflowException {
        switch (dataFormat) {
            case "mp3":
                return FileDataDeidentifyAudioDataFormat.MP_3;
            case "wav":
                return FileDataDeidentifyAudioDataFormat.WAV;
            default:
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidAudioFileType.getMessage());
        }
    }

    private void updateVaultURL() {
        String vaultURL = Utils.getVaultURL(this.vaultConfig.getClusterId(), this.vaultConfig.getEnv());
        if (!vaultURL.equals(this.currentVaultURL)) {
            this.currentVaultURL = vaultURL;
            this.apiClientBuilder.url(vaultURL);
            this.apiClient = null;
        }
    }

    private void updateExecutorInHTTP() {
        if (sharedHttpClient == null) {
            sharedHttpClient = new OkHttpClient.Builder()
                    .connectionPool(new ConnectionPool(Constants.HTTP_MAX_IDLE_CONNECTIONS,
                            Constants.HTTP_KEEP_ALIVE_DURATION_MINUTES, TimeUnit.MINUTES))
                    .addInterceptor(chain -> {
                        Request requestWithAuth = chain.request().newBuilder()
                                .header("Authorization", "Bearer " + this.token)
                                .build();
                        return chain.proceed(requestWithAuth);
                    })
                    .build();
            apiClientBuilder.httpClient(sharedHttpClient);
        }
    }

    private void prioritiseCredentials() throws SkyflowException {
        try {
            Credentials original = this.finalCredentials;
            if (this.vaultConfig.getCredentials() != null) {
                this.finalCredentials = this.vaultConfig.getCredentials();
            } else if (this.commonCredentials != null) {
                this.finalCredentials = this.commonCredentials;
            } else {
                Dotenv dotenv = Dotenv.load();
                String sysCredentials = dotenv.get(Constants.ENV_CREDENTIALS_KEY_NAME);
                if (sysCredentials == null) {
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.EmptyCredentials.getMessage());
                } else {
                    this.finalCredentials = new Credentials();
                    this.finalCredentials.setCredentialsString(sysCredentials);
                }
            }
            if (original != null && !original.equals(this.finalCredentials)) {
                token = null;
            }
        } catch (DotenvException e) {
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.EmptyCredentials.getMessage());
        } catch (SkyflowException e) {
            throw e;
        } catch (Exception e) {
            throw new SkyflowException(ErrorCode.SERVER_ERROR.getCode(), ErrorMessage.EmptyCredentials.getMessage());
        }
    }
}
