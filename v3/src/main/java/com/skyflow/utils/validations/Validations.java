package com.skyflow.utils.validations;

import java.util.ArrayList;
import java.util.List;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.InterfaceName;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.DetokenizeRequest;
import com.skyflow.vault.data.InsertRecord;
import com.skyflow.vault.data.InsertRequest;
import com.skyflow.vault.data.TokenGroupRedactions;

public class Validations extends BaseValidations {
    private Validations() {
        super();
    }

    public static void validateInsertRequest(InsertRequest insertRequest) throws SkyflowException {
        String table = insertRequest.getTable();
        ArrayList<InsertRecord> records = insertRequest.getRecords();
        if (records == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.RECORDS_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RecordsKeyError.getMessage());
        } else if (records.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_RECORDS.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRecords.getMessage());
        } else if (records.size() > 10000) {
            LogUtil.printErrorLog(ErrorLogs.RECORD_SIZE_EXCEED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RecordSizeExceedError.getMessage());
        }
        for (InsertRecord record : records) {
            if(record == null){
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INVALID_RECORD.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidRecord.getMessage());
            }
        }

        // table check if specified for both
        if (insertRequest.getTable() != null && !table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord record : records) {
                if (record.getTable() != null && !record.getTable().trim().isEmpty()){
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TABLE_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage());
                }
            }
        }
        // table check if not specified for both or if missing in any object
        if (insertRequest.getTable() == null || table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord record : records) {
                if (record.getTable() == null || record.getTable().trim().isEmpty()){
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TABLE_NOT_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage());
                }
            }
        }
        // upsert check 1
        if (insertRequest.getTable() != null && !table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord record : records) {
                if (record.getUpsert() != null && record.getUpsert().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_UPSERT_VALUES.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUpsertValues.getMessage());
                }
                if (record.getUpsert() != null && !record.getUpsert().isEmpty()){
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.UPSERT_TABLE_REQUEST_AT_RECORD_LEVEL.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage());
                }
            }
        }
        // upsert check 2
        if (insertRequest.getTable() == null || table.trim().isEmpty()){
            if (insertRequest.getUpsert() != null && !insertRequest.getUpsert().isEmpty()){
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.UPSERT_TABLE_REQUEST_AT_REQUEST_LEVEL.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage());
            }
        }

        if (insertRequest.getUpsert() != null && insertRequest.getUpsert().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_UPSERT_VALUES.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUpsertValues.getMessage());
        }

        for (InsertRecord record : records) {
            if (record != null ) {
                if (record.getData() != null){
                    for (String key : record.getData().keySet()) {
                        if (key == null || key.trim().isEmpty()) {
                            LogUtil.printErrorLog(Utils.parameterizedString(
                                    ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.INSERT.getName()
                            ));
                            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInRecords.getMessage());
                        } else {
                            Object value = record.getData().get(key);
                            if (value == null || value.toString().trim().isEmpty()) {
                                LogUtil.printErrorLog(Utils.parameterizedString(
                                        ErrorLogs.EMPTY_OR_NULL_VALUE_IN_VALUES.getLog(),
                                        InterfaceName.INSERT.getName(), key
                                ));
                                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInValues.getMessage());
                            }
                        }
                    }
                }
            }
        }
    }

    public static void validateDetokenizeRequest(DetokenizeRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.DETOKENIZE_REQUEST_NULL.getLog(), InterfaceName.DETOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.DetokenizeRequestNull.getMessage());
        }
        List<String> tokens = request.getTokens();
        if (tokens.size() > 10000) {
            LogUtil.printErrorLog(ErrorLogs.TOKENS_SIZE_EXCEED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokensSizeExceedError.getMessage());
        }
        if (tokens == null || tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_DETOKENIZE_DATA.getLog(), InterfaceName.DETOKENIZE.getName()
        ));
        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyDetokenizeData.getMessage());
        }
        for (int index = 0; index < tokens.size(); index++) {
            String token = tokens.get(index);
            if (token == null || token.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_OR_NULL_TOKEN_IN_DETOKENIZE_DATA.getLog(),
                    InterfaceName.DETOKENIZE.getName(),
                    String.valueOf(index)));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenInDetokenizeData.getMessage());
            }
        }

        List<TokenGroupRedactions> groupRedactions = request.getTokenGroupRedactions();
        if (groupRedactions != null && !groupRedactions.isEmpty()) {
        for (TokenGroupRedactions group : groupRedactions) {
            if (group == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(ErrorLogs.NULL_TOKEN_REDACTION_GROUP_OBJECT.getLog(), InterfaceName.DETOKENIZE.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullTokenGroupRedactions.getMessage());
            }
            String groupName = group.getTokenGroupName();
            String redaction = group.getRedaction();
            if (groupName == null || groupName.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(ErrorLogs.NULL_TOKEN_GROUP_NAME_IN_TOKEN_GROUP.getLog(), InterfaceName.DETOKENIZE.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullTokenGroupNameInTokenGroup.getMessage());
            }
            if (redaction == null || redaction.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(ErrorLogs.EMPTY_OR_NULL_REDACTION_IN_TOKEN_GROUP.getLog(), InterfaceName.DETOKENIZE.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullRedactionInTokenGroup.getMessage());
            }
        }
        }
    }

    public static void validateVaultConfiguration(VaultConfig vaultConfig) throws SkyflowException {
        String vaultId = vaultConfig.getVaultId();
        String clusterId = vaultConfig.getClusterId();
        String vaultURL = vaultConfig.getVaultURL();
        Credentials credentials = vaultConfig.getCredentials();

        if (vaultId == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_ID_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultId.getMessage());
        } else if (vaultId.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_ID.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultId.getMessage());
        } else if (credentials != null) {
            validateCredentials(credentials);
        }

        if (vaultURL != null) {
            if (vaultURL.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_URL.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultUrl.getMessage());
            } else if (!Utils.isValidURL(vaultURL)) {
                LogUtil.printErrorLog(ErrorLogs.INVALID_VAULT_URL_FORMAT.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultUrlFormat.getMessage());
            }
        } else if (Utils.getEnvVaultURL() == null) {
            if (clusterId == null) {
                LogUtil.printErrorLog(ErrorLogs.EITHER_VAULT_URL_OR_CLUSTER_ID_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EitherVaultUrlOrClusterIdRequired.getMessage());
            } else if (clusterId.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_CLUSTER_ID.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyClusterId.getMessage());
            }
        }
    }
}
