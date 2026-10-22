package com.skyflow;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.InterfaceName;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.generated.rest.ApiClient;
import com.skyflow.generated.rest.ApiClientBuilder;
import com.skyflow.generated.rest.resources.query.QueryClient;
import com.skyflow.generated.rest.resources.records.RecordsClient;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceBatchOperationBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceInsertRecordBody;
import com.skyflow.generated.rest.resources.records.requests.RecordServiceUpdateRecordBody;
import com.skyflow.generated.rest.resources.tokens.TokensClient;
import com.skyflow.generated.rest.resources.tokens.requests.V1DetokenizePayload;
import com.skyflow.generated.rest.resources.tokens.requests.V1TokenizePayload;
import com.skyflow.generated.rest.types.*;
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
            sharedHttpClient = buildSharedHttpClient(() -> this.token);
            apiClientBuilder.httpClient(sharedHttpClient);
        }
    }

}