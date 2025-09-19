package com.skyflow.utils.validations;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Validations extends BaseValidations {
    private Validations() {
        super();
    }

    public static void validateInsertRequest(InsertRequest insertRequest) throws SkyflowException {
        String table = insertRequest.getTable();
        ArrayList<InsertRecord> values = insertRequest.getRecords();
        if (values == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.VALUES_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ValuesKeyError.getMessage());
        } else if (values.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_VALUES.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValues.getMessage());
        } else if (values.size() > 10000) {
            LogUtil.printErrorLog(ErrorLogs.RECORD_SIZE_EXCEED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RecordSizeExceedError.getMessage());
        }
//        if (table == null) {
//            LogUtil.printErrorLog(Utils.parameterizedString(
//                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
//            ));
//            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
//        } else if (table.trim().isEmpty()) {
//            LogUtil.printErrorLog(Utils.parameterizedString(
//                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.INSERT.getName()
//            ));
//            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
//        }
        // table check if specified for both
        if (insertRequest.getTable() != null && !table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord valuesMap : values) {
                if (valuesMap.getTable() != null || !valuesMap.getTable().trim().isEmpty()){
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TABLE_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage());
                }
            }
        }
        // table check if not specified for both or if missing in any object
        if (insertRequest.getTable() == null || table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord valuesMap : values) {
                if (valuesMap.getTable() == null || valuesMap.getTable().trim().isEmpty()){
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TABLE_NOT_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage());
                }
            }
        }
        // upsert check 1
        if (insertRequest.getTable() != null && !table.trim().isEmpty()){ // if table name specified at both place
            for (InsertRecord valuesMap : values) {
                if (valuesMap.getUpsert() != null && !valuesMap.getUpsert().isEmpty()){
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

//        else if (upsert != null && upsert.isEmpty()) {
//            LogUtil.printErrorLog(Utils.parameterizedString(
//                    ErrorLogs.EMPTY_UPSERT_VALUES.getLog(), InterfaceName.INSERT.getName()
//            ));
//            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUpsertValues.getMessage());
//        }

        for (InsertRecord valuesMap : values) {
            if (valuesMap != null ) {
                if (valuesMap.getData() != null){
                    for (String key : valuesMap.getData().keySet()) {
                        if (key == null || key.trim().isEmpty()) {
                            LogUtil.printErrorLog(Utils.parameterizedString(
                                    ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.INSERT.getName()
                            ));
                            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInValues.getMessage());
                        } else {
                            Object value = valuesMap.getData().get(key);
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
        for (String token : tokens) {
            if (token == null || token.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_TOKEN_IN_DETOKENIZE_DATA.getLog(), InterfaceName.DETOKENIZE.getName()
                ));
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
        Credentials credentials = vaultConfig.getCredentials();
        if (vaultId == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_ID_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultId.getMessage());
        } else if (vaultId.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_ID.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultId.getMessage());
        } else if (Utils.getEnvVaultURL() == null) {
            if (clusterId == null) {
                LogUtil.printErrorLog(ErrorLogs.CLUSTER_ID_IS_REQUIRED.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidClusterId.getMessage());
            } else if (clusterId.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_CLUSTER_ID.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyClusterId.getMessage());
            }
        } else if (credentials != null) {
            validateCredentials(credentials);
        }
    }
}
