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
import com.skyflow.generated.rest.resources.strings.types.ReidentifyStringRequestFormat;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.*;
import com.skyflow.generated.rest.types.Transformations;
import com.skyflow.logs.InfoLogs;
import com.skyflow.serviceaccount.util.Token;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.utils.validations.Validations;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.detect.*;
import com.skyflow.vault.detect.DeidentifyFileRequest;
import com.skyflow.vault.detect.DeidentifyTextRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;
import io.github.cdimascio.dotenv.Dotenv;
import io.github.cdimascio.dotenv.DotenvException;

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

    protected FilesClient getDetectFileAPi(){
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
            if (value != null) {
                recordBuilder.value(value);
            }
            String columnGroup = columnValue.getColumnGroup();
            if (columnGroup != null) {
                recordBuilder.columnGroup(columnGroup);
            }

            tokenizationParameters.add(recordBuilder.build());
        }

        V1TokenizePayload.Builder payloadBuilder = V1TokenizePayload.builder();

        if (!tokenizationParameters.isEmpty()) {
            payloadBuilder.tokenizationParameters(tokenizationParameters);
        }

        return payloadBuilder.build();
    }

    protected void setBearerToken() throws SkyflowException {
        prioritiseCredentials();
        Validations.validateCredentials(this.finalCredentials);
        if (this.finalCredentials.getApiKey() != null) {
            setApiKey();
            return;
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
        List<EntityInfo> entities = deidentifyStringResponse.getEntities() != null
                ? deidentifyStringResponse.getEntities().stream()
                .map(this::convertDetectedEntityToEntityInfo)
                .collect(Collectors.toList())
                : null;

        return new DeidentifyTextResponse(
                deidentifyStringResponse.getProcessedText(),
                entities,
                deidentifyStringResponse.getWordCount(),
                deidentifyStringResponse.getCharacterCount()
        );
    }

    protected DeidentifyStringRequest getDeidentifyStringRequest(DeidentifyTextRequest deIdentifyTextRequest, String vaultId) throws SkyflowException {
        List<DetectEntities> entities = deIdentifyTextRequest.getEntities();

        List<EntityType> mappedEntityTypes = null;
        if (entities != null) {
            mappedEntityTypes = deIdentifyTextRequest.getEntities().stream()
                    .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        TokenFormat tokenFormat = deIdentifyTextRequest.getTokenFormat();

        Optional<List<EntityType>> vaultToken = Optional.empty();
        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(deIdentifyTextRequest.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(deIdentifyTextRequest.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(deIdentifyTextRequest.getTransformations()));

        if (tokenFormat != null) {
            if (tokenFormat.getVaultToken() != null && !tokenFormat.getVaultToken().isEmpty()) {
                vaultToken = Optional.of(tokenFormat.getVaultToken().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenType tokenType = TokenType.builder()
                .vaultToken(vaultToken)
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();


        return DeidentifyStringRequest.builder()
                .vaultId(vaultId)
                .text(deIdentifyTextRequest.getText())
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected ReidentifyStringRequest getReidentifyStringRequest(ReidentifyTextRequest reidentifyTextRequest, String vaultId) throws SkyflowException {
        List<EntityType> maskEntities = null;
        List<EntityType> redactedEntities = null;
        List<EntityType> plaintextEntities = null;

        if (reidentifyTextRequest.getMaskedEntities() != null) {
            maskEntities = reidentifyTextRequest.getMaskedEntities().stream()
                    .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        if (reidentifyTextRequest.getPlainTextEntities() != null) {
            plaintextEntities = reidentifyTextRequest.getPlainTextEntities().stream()
                    .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        if (reidentifyTextRequest.getRedactedEntities() != null) {
            redactedEntities = reidentifyTextRequest.getRedactedEntities().stream()
                    .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        ReidentifyStringRequestFormat reidentifyStringRequestFormat = ReidentifyStringRequestFormat.builder()
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


    private EntityInfo convertDetectedEntityToEntityInfo(DetectedEntity detectedEntity) {
        TextIndex textIndex = new TextIndex(
                detectedEntity.getLocation().get().getStartIndex().orElse(0),
                detectedEntity.getLocation().get().getEndIndex().orElse(0)
        );
        TextIndex processedIndex = new TextIndex(
                detectedEntity.getLocation().get().getStartIndexProcessed().orElse(0),
                detectedEntity.getLocation().get().getEndIndexProcessed().orElse(0)
        );

        Map<String, Float> entityScores = detectedEntity.getEntityScores()
                .map(doubleMap -> doubleMap.entrySet().stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().floatValue()
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

        List<TransformationsShiftDatesEntityTypesItem> entityTypes = null;
        if (!transformations.getShiftDates().getEntities().isEmpty()) {
            entityTypes = transformations.getShiftDates().getEntities().stream()
                    .map(entity -> TransformationsShiftDatesEntityTypesItem.valueOf(entity.name()))
                    .collect(Collectors.toList());
        } else {
            entityTypes = Collections.emptyList();
        }

        return Transformations.builder()
                .shiftDates(TransformationsShiftDates.builder()
                        .maxDays(transformations.getShiftDates().getMax())
                        .minDays(transformations.getShiftDates().getMin())
                        .entityTypes(entityTypes)
                        .build())
                .build();
    }

    private List<EntityType> getEntityTypes(List<DetectEntities> entities){
        List<EntityType> mappedEntityTypes = null;
        if (entities != null) {
            mappedEntityTypes = entities.stream()
                    .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                    .collect(Collectors.toList());
        }

        return mappedEntityTypes;
    }

    protected com.skyflow.generated.rest.resources.files.requests.DeidentifyTextRequest getDeidentifyTextFileRequest(DeidentifyFileRequest request, String vaultId, String base64Content){
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {

            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenTypeWithoutVault tokenType = TokenTypeWithoutVault.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

        DeidentifyTextRequestFile file = DeidentifyTextRequestFile.builder()
                .base64(base64Content)
                .build();

        // Build the final request
        com.skyflow.generated.rest.resources.files.requests.DeidentifyTextRequest req =
                com.skyflow.generated.rest.resources.files.requests.DeidentifyTextRequest.builder()
                        .vaultId(vaultId)
                        .file(file)
                        .entityTypes(mappedEntityTypes)
                        .tokenType(tokenType)
                        .allowRegex(allowRegex)
                        .restrictRegex(restrictRegex)
                        .transformations(transformations)
                        .build();

        return req;
    }


    protected DeidentifyAudioRequest getDeidentifyAudioRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String dataFormat){
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());

        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {

            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenTypeWithoutVault tokenType = TokenTypeWithoutVault.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

        DeidentifyAudioRequestFile deidentifyAudioRequestFile = DeidentifyAudioRequestFile.builder().base64(base64Content).dataFormat(DeidentifyAudioRequestFileDataFormat.valueOf(dataFormat)).build();
        DetectOutputTranscriptions transcription = request.getOutputTranscription();
        DeidentifyAudioRequestOutputTranscription outputTranscriptionType = null;
        if (transcription != null) {
            outputTranscriptionType = DeidentifyAudioRequestOutputTranscription.valueOf(transcription.name());
        }

        return  DeidentifyAudioRequest.builder()
                .vaultId(vaultId)
                .file(deidentifyAudioRequestFile)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .entityTypes(mappedEntityTypes)
                .bleepFrequency(request.getBleep().getFrequency())
                .bleepGain(request.getBleep().getGain())
                .bleepStartPadding(request.getBleep().getStartPadding())
                .bleepStopPadding(request.getBleep().getStopPadding())
                .outputProcessedAudio(request.getOutputProcessedAudio())
                .outputTranscription(outputTranscriptionType)
                .tokenType(tokenType)
                .transformations(transformations)
                .build();
    }

    // Add to VaultClient.java class
    protected DeidentifyPdfRequest getDeidentifyPdfRequest(DeidentifyFileRequest request, String vaultId, String base64Content) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());

        TokenFormat tokenFormat = request.getTokenFormat();
        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        TokenTypeWithoutVault tokenType = TokenTypeWithoutVault.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();

        DeidentifyPdfRequestFile file = DeidentifyPdfRequestFile.builder()
                .base64(base64Content)
                .build();

        return DeidentifyPdfRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .density(request.getPixelDensity() != null ? request.getPixelDensity().intValue() : null)
                .maxResolution(request.getMaxResolution() != null ? request.getMaxResolution().intValue() : null)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyImageRequest getDeidentifyImageRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());

        TokenFormat tokenFormat = request.getTokenFormat();
        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeWithoutVault tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

        DeidentifyImageRequestFile file = DeidentifyImageRequestFile.builder()
                .base64(base64Content)
                .dataFormat(DeidentifyImageRequestFileDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyImageRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .outputProcessedImage(request.getOutputProcessedImage())
                .outputOcrText(request.getOutputOcrText())
                .build();
    }

    protected DeidentifyPresentationRequest getDeidentifyPresentationRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeWithoutVault tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

        DeidentifyPresentationRequestFile file = DeidentifyPresentationRequestFile.builder()
                .base64(base64Content)
                .dataFormat(DeidentifyPresentationRequestFileDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyPresentationRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected DeidentifySpreadsheetRequest getDeidentifySpreadsheetRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeWithoutVault tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

        DeidentifySpreadsheetRequestFile file = DeidentifySpreadsheetRequestFile.builder()
                .base64(base64Content)
                .dataFormat(DeidentifySpreadsheetRequestFileDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifySpreadsheetRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyStructuredTextRequest getDeidentifyStructuredTextRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeWithoutVault tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

        DeidentifyStructuredTextRequestFile file = DeidentifyStructuredTextRequestFile.builder()
                .base64(base64Content)
                .dataFormat(DeidentifyStructuredTextRequestFileDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyStructuredTextRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    protected DeidentifyDocumentRequest getDeidentifyDocumentRequest(DeidentifyFileRequest request, String vaultId, String base64Content, String format) {
        List<EntityType> mappedEntityTypes = getEntityTypes(request.getEntities());
        TokenFormat tokenFormat = request.getTokenFormat();

        Optional<List<EntityType>> entityTypes = Optional.empty();
        Optional<List<EntityType>> entityUniqueCounter = Optional.empty();
        Optional<List<String>> allowRegex = Optional.ofNullable(request.getAllowRegexList());
        Optional<List<String>> restrictRegex = Optional.ofNullable(request.getRestrictRegexList());
        Optional<Transformations> transformations = Optional.ofNullable(getTransformations(request.getTransformations()));

        TokenTypeWithoutVault tokenType = buildTokenType(tokenFormat, entityTypes, entityUniqueCounter);

        DeidentifyDocumentRequestFile file = DeidentifyDocumentRequestFile.builder()
                .base64(base64Content)
                .dataFormat(DeidentifyDocumentRequestFileDataFormat.valueOf(format.toUpperCase()))
                .build();

        return DeidentifyDocumentRequest.builder()
                .vaultId(vaultId)
                .file(file)
                .entityTypes(mappedEntityTypes)
                .tokenType(tokenType)
                .allowRegex(allowRegex)
                .restrictRegex(restrictRegex)
                .transformations(transformations)
                .build();
    }

    // Helper method to build TokenType
    private TokenTypeWithoutVault buildTokenType(TokenFormat tokenFormat,
                                                 Optional<List<EntityType>> entityTypes,
                                                 Optional<List<EntityType>> entityUniqueCounter) {

        if (tokenFormat != null) {
            if (tokenFormat.getEntityOnly() != null && !tokenFormat.getEntityOnly().isEmpty()) {
                entityTypes = Optional.of(tokenFormat.getEntityOnly().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }

            if (tokenFormat.getEntityUniqueCounter() != null && !tokenFormat.getEntityUniqueCounter().isEmpty()) {
                entityUniqueCounter = Optional.of(tokenFormat.getEntityUniqueCounter().stream()
                        .map(detectEntity -> EntityType.valueOf(detectEntity.name()))
                        .collect(Collectors.toList()));
            }
        }

        return TokenTypeWithoutVault.builder()
                .entityOnly(entityTypes)
                .entityUnqCounter(entityUniqueCounter)
                .build();
    }


    private void setApiKey() {
        if (apiKey == null) {
            apiKey = this.finalCredentials.getApiKey();
        } else {
            LogUtil.printInfoLog(InfoLogs.REUSE_API_KEY.getLog());
        }
        this.apiClientBuilder.token(token);
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
