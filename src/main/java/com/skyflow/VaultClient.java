package com.skyflow;

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
import com.skyflow.generated.rest.resources.files.requests.*;
import com.skyflow.generated.rest.resources.files.types.*;
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
import com.skyflow.generated.rest.types.Transformations;
import com.skyflow.generated.rest.types.*;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.*;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


public class VaultClient {
    private final VaultConfig vaultConfig;
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;
    private Credentials commonCredentials;
    private Credentials finalCredentials;
    private String token;
    private String apiKey;

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
                .downloadUrl(request.getDownloadURL())
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
                .byot(request.getTokenMode().getBYOT())
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
                .byot(request.getTokenMode().getBYOT())
                .records(records)
                .build();
    }

    protected RecordServiceUpdateRecordBody getUpdateRequestBody(UpdateRequest request) {
        RecordServiceUpdateRecordBody.Builder updateRequestBodyBuilder = RecordServiceUpdateRecordBody.builder();
        updateRequestBodyBuilder.byot(request.getTokenMode().getBYOT());
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

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
            token = this.finalCredentials.getApiKey();
        } else if (Token.isExpired(token)) {
            LogUtil.printInfoLog(InfoLogs.BEARER_TOKEN_EXPIRED.getLog());
            token = Utils.generateBearerToken(this.finalCredentials);
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_BEARER_TOKEN.getLog());
        }
        this.apiClientBuilder.token(token);
        this.apiClient = this.apiClientBuilder.build();
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

        Optional<List<TokenTypeMappingVaultTokenItem>> vaultToken = Optional.empty();
        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(deIdentifyTextRequest.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(deIdentifyTextRequest.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(deIdentifyTextRequest.getTransformations()));

        if (tokenFormat != null) {
            if (tokenFormat.getVaultToken() != null && !tokenFormat.getVaultToken().isEmpty()) {
                vaultToken = Optional.of(
                        tokenFormat.getVaultToken().stream()
                                .map(detectEntity -> TokenTypeMappingVaultTokenItem.valueOf(detectEntity.name()))
                                .collect(Collectors.toList())
                );
            }


            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(
                        tokenFormat.getEntityOnly().stream()
                                .map(detectEntity -> TokenTypeMappingEntityOnlyItem.valueOf(detectEntity.name()))
                                .collect(Collectors.toList())
                );
            }


            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(
                        tokenFormat.getEntityUniqueCounter().stream()
                                .map(detectEntity -> TokenTypeMappingEntityUnqCounterItem.valueOf(detectEntity.name()))
                                .collect(Collectors.toList())
                );
            }

        }

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .vaultToken(vaultToken)
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
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
        Optional<List<FormatMaskedItem>> maskEntities = null;
        Optional<List<FormatRedactedItem>> redactedEntities = null;
        Optional<List<FormatPlaintextItem>> plaintextEntities = null;

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
                .map(doubleMap -> doubleMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue
                        )))
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

        Optional<List<ShiftDatesEntityTypesItem>> entityTypes = null;
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
        if (entities == null) return Collections.emptyList();

        return entities.stream()
                .map(e -> Enum.valueOf(type, e.name()))
                .collect(Collectors.toList());
    }


    protected com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText getDeidentifyTextFileRequest(DeidentifyFileRequest request, String vaultId, String base64Content) {
        List<DeidentifyFileRequestDeidentifyTextEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileRequestDeidentifyTextEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {

            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> TokenTypeMappingEntityOnlyItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> TokenTypeMappingEntityUnqCounterItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

        FileDataDeidentifyText file = FileDataDeidentifyText.builder()
                .base64(base64Content)
                .build();

        // Build the final request
        com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText req =
                com.skyflow.generated.rest.resources.files.requests.DeidentifyFileRequestDeidentifyText.builder()
                        .file(file)
                        .vaultId(vaultId)
                        .entityTypes(mappedEntityTypes)
                        .tokenType(tokenType)
                        .allowRegex(allowRegex)
                        .restrictRegex(restrictRegex)
                        .transformations(transformations)
                        .build();

        return req;
    }


    protected DeidentifyFileAudioRequestDeidentifyAudio getDeidentifyAudioRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String dataFormat) throws SkyflowException {
        List<DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(
                        tokenFormat.getEntityOnly().stream()
                                .map(detectEntity -> DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem.valueOf(detectEntity.name()))
                                .collect(Collectors.toList())
                ).map(list -> (List<TokenTypeMappingEntityOnlyItem>) (List<?>) list);
            }


            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(
                        (List<TokenTypeMappingEntityUnqCounterItem>) (List<?>)
                                tokenFormat.getEntityUniqueCounter().stream()
                                        .map(detectEntity -> DeidentifyFileAudioRequestDeidentifyAudioEntityTypesItem.valueOf(detectEntity.name()))
                                        .collect(Collectors.toList())
                );
            }

        }

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

        FileDataDeidentifyAudio deidentifyAudioRequestFile = FileDataDeidentifyAudio.builder().base64(base64Content).dataFormat(mapAudioDataFormat(dataFormat)).build();
        DetectOutputTranscriptions transcription = request.getOutputTranscription();
        Optional<DeidentifyFileAudioRequestDeidentifyAudioOutputTranscription> outputTranscriptionType = null;
        if (transcription != null) {
            outputTranscriptionType = Optional.of(DeidentifyFileAudioRequestDeidentifyAudioOutputTranscription.valueOf(transcription.name()));
        }

        return DeidentifyFileAudioRequestDeidentifyAudio.builder()
                .file(deidentifyAudioRequestFile)
                .vaultId(vaultId)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .entityTypes(mappedEntityTypes)
                .bleepFrequency(request.getBleep() != null
                        ? (int) Math.round(request.getBleep().getFrequency())
                        : null)
                .bleepGain(request.getBleep() != null ? (int) Math.round(request.getBleep().getGain()) : null)
                .bleepStartPadding(request.getBleep() != null ? (float) Math.round(request.getBleep().getStartPadding()) : null)
                .bleepStopPadding(request.getBleep() != null ? (float) Math.round(request.getBleep().getStopPadding()) : null)
                .outputProcessedAudio(request.getOutputProcessedAudio())
                .outputTranscription(outputTranscriptionType)
                .tokenType(tokenType)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyFileDocumentPdfRequestDeidentifyPdf getDeidentifyPdfRequest(DeidentifyFileRequest request, String vaultId, String base64Content) {
        List<DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem> mappedEntityTypes = getEntityTypes(request.getEntities(), DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem.class);

        TokenFormat tokenFormat = request.getTokenFormat();
        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(
                        (List<TokenTypeMappingEntityOnlyItem>) (List<?>)
                                tokenFormat.getEntityOnly().stream()
                                        .map(detectEntity -> DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem.valueOf(detectEntity.name()))
                                        .collect(Collectors.toList())
                );
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(
                        (List<TokenTypeMappingEntityUnqCounterItem>) (List<?>)
                                tokenFormat.getEntityUniqueCounter().stream()
                                        .map(detectEntity -> DeidentifyFileDocumentPdfRequestDeidentifyPdfEntityTypesItem.valueOf(detectEntity.name()))
                                        .collect(Collectors.toList())
                );
            }

        }

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

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
        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

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

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

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

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

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

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeMapping tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

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

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());

        TokenTypeMapping tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

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

        Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes = Optional.empty();
        Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> TokenTypeMappingEntityOnlyItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> TokenTypeMappingEntityUnqCounterItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenTypeMapping tokenType = TokenTypeMapping.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

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

    private TokenTypeMapping buildTokenType(TokenFormat tokenFormat,
                                            Optional<List<TokenTypeMappingEntityOnlyItem>> entityTypes,
                                            Optional<List<TokenTypeMappingEntityUnqCounterItem>> entityUniqueCounter) {

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> TokenTypeMappingEntityOnlyItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> TokenTypeMappingEntityUnqCounterItem.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        return TokenTypeMapping.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

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
        this.apiClientBuilder.url(vaultURL);
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
                apiKey = null;
            }
        } catch (DotenvException e) {
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.EmptyCredentials.getMessage());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
