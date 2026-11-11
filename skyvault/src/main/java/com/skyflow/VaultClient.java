package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.InterfaceName;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.files.FilesClient;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.BatchOperationRequest;
import com.skyflow.generated.rest.resources.records.requests.InsertRecordRequest;
import com.skyflow.generated.rest.resources.records.requests.UpdateRecordRequest;
import com.skyflow.generated.rest.resources.records.types.InsertRecordRequestByot;
import com.skyflow.generated.rest.resources.records.types.UpdateRecordRequestByot;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.TokenizePayload;
import com.skyflow.generated.rest.types.BatchRecord;
import com.skyflow.generated.rest.types.BatchRecordMethod;
import com.skyflow.generated.rest.types.DetokenizeRecordRequest;
import com.skyflow.generated.rest.types.DetokenizeRecordRequestRedaction;
import com.skyflow.generated.rest.types.FieldRecords;
import com.skyflow.generated.rest.types.TokenizeRecordRequest;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.FileUploadRequest;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.UpdateRequest;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeData;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;


public class VaultClient extends BaseVaultClient<VaultConfig> {

    /**
     * Restores the concrete descriptor from skyflow-java 2.1.1. BaseVaultClient declares this as
     * {@code V getVaultConfig()}, which erases to BaseVaultConfig; VaultController and
     * DetectController are compiled against the concrete form.
     */
    @Override
    protected VaultConfig getVaultConfig() {
        return super.getVaultConfig();
    }
    private final ApiClientBuilder apiClientBuilder;
    private ApiClient apiClient;

    protected VaultClient(VaultConfig vaultConfig, Credentials credentials) {
        super(vaultConfig, credentials);
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

    protected QueryClient getQueryApi() {
        return this.apiClient.query();
    }

    protected FilesClient getFilesApi() {
        return this.apiClient.files();
    }

    protected void setCommonCredentials(Credentials commonCredentials) throws SkyflowException {
        this.commonCredentials = commonCredentials;
        super.prioritiseCredentials(this.vaultConfig.getCredentials());
    }

    protected void updateVaultConfig() throws SkyflowException {
        updateVaultURL();
        super.prioritiseCredentials(this.vaultConfig.getCredentials());
    }

    protected void updateVaultConfig(VaultConfig newConfig) throws SkyflowException {
        this.vaultConfig = newConfig;
        updateVaultConfig();
    }

    protected DetokenizePayload getDetokenizePayload(DetokenizeRequest request) {
        List<DetokenizeRecordRequest> recordRequests = new ArrayList<>();

        for (DetokenizeData detokenizeDataRecord : request.getDetokenizeData()) {
            DetokenizeRecordRequest recordRequest = DetokenizeRecordRequest.builder()
                    .token(detokenizeDataRecord.getToken())
                    .redaction(DetokenizeRecordRequestRedaction.valueOf(detokenizeDataRecord.getRedactionType().toString()))
                    .build();
            recordRequests.add(recordRequest);
        }

        return DetokenizePayload.builder()
                .continueOnError(request.getContinueOnError())
                .downloadUrl(request.getDownloadUrl())
                .detokenizationParameters(recordRequests)
                .build();
    }

    protected InsertRecordRequest getBulkInsertRequestBody(InsertRequest request) {
        List<HashMap<String, Object>> values = request.getValues();
        List<HashMap<String, Object>> tokens = request.getTokens();
        List<FieldRecords> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            FieldRecords.Builder recordBuilder = FieldRecords.builder().fields(values.get(index));
            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }
            records.add(recordBuilder.build());
        }

        return InsertRecordRequest.builder()
                .tokenization(request.getReturnTokens())
                .homogeneous(request.getHomogeneous())
                .upsert(request.getUpsert())
                .byot(InsertRecordRequestByot.valueOf(request.getTokenMode().toString()))
                .records(records)
                .build();
    }

    protected BatchOperationRequest getBatchInsertRequestBody(InsertRequest request) {
        ArrayList<HashMap<String, Object>> values = request.getValues();
        ArrayList<HashMap<String, Object>> tokens = request.getTokens();
        List<BatchRecord> records = new ArrayList<>();

        for (int index = 0; index < values.size(); index++) {
            BatchRecord._FinalStage recordBuilder = BatchRecord.builder()
                    .tableName(request.getTable())
                    .method(BatchRecordMethod.POST)
                    .upsert(request.getUpsert())
                    .tokenization(request.getReturnTokens())
                    .fields(values.get(index));

            if (tokens != null && index < tokens.size()) {
                recordBuilder.tokens(tokens.get(index));
            }

            records.add(recordBuilder.build());
        }

        // The regenerated batch-operation request has no byot field; keep sending it as before so the
        // BYOT behaviour of insert(continueOnError=true) is unchanged.
        return BatchOperationRequest.builder()
                .continueOnError(true)
                .records(records)
                .additionalProperty("byot", request.getTokenMode().toString())
                .build();
    }

    protected UpdateRecordRequest getUpdateRequestBody(UpdateRequest request) {
        UpdateRecordRequest.Builder updateRequestBodyBuilder = UpdateRecordRequest.builder();
        updateRequestBodyBuilder.byot(UpdateRecordRequestByot.valueOf(request.getTokenMode().toString()));
        updateRequestBodyBuilder.tokenization(request.getReturnTokens());
        FieldRecords.Builder recordBuilder = FieldRecords.builder();
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

    protected TokenizePayload getTokenizePayload(TokenizeRequest request) {
        List<TokenizeRecordRequest> tokenizationParameters = new ArrayList<>();

        for (ColumnValue columnValue : request.getColumnValues()) {
            TokenizeRecordRequest.Builder recordBuilder = TokenizeRecordRequest.builder();
            String value = columnValue.getValue();
            recordBuilder.value(value);
            String columnGroup = columnValue.getColumnGroup();
            recordBuilder.columnGroup(columnGroup);
            tokenizationParameters.add(recordBuilder.build());
        }

        return TokenizePayload.builder()
                .tokenizationParameters(tokenizationParameters)
                .build();
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
        super.setBearerToken(this.vaultConfig.getCredentials());
        if (apiClient == null) {
            updateExecutorInHTTP();
            this.apiClient = this.apiClientBuilder.build();
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
            sharedHttpClient = buildSharedHttpClient(this::getToken);
            apiClientBuilder.httpClient(sharedHttpClient);
        }
    }

}