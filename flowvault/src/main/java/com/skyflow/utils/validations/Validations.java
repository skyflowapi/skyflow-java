package com.skyflow.utils.validations;

import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.InterfaceName;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.data.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validations extends BaseValidations {
    private Validations() {
        super();
    }

    public static void validateCredentials(Credentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        String path = credentials.getPath();
        String credentialsString = credentials.getCredentialsString();
        String token = credentials.getToken();
        String apiKey = credentials.getApiKey();
        Object context = credentials.getContext();
        ArrayList<String> roles = credentials.getRoles();

        if (path != null) nonNullMembers++;
        if (credentialsString != null) nonNullMembers++;
        if (token != null) nonNullMembers++;
        if (apiKey != null) nonNullMembers++;

        if (nonNullMembers > 1) {
            LogUtil.printErrorLog(ErrorLogs.MULTIPLE_TOKEN_GENERATION_MEANS_PASSED.getLog());
            throw new SkyflowException(
                    ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MultipleTokenGenerationMeansPassed.getMessage()
            );
        } else if (nonNullMembers < 1) {
            LogUtil.printErrorLog(ErrorLogs.NO_TOKEN_GENERATION_MEANS_PASSED.getLog());
            throw new SkyflowException(
                    ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NoTokenGenerationMeansPassed.getMessage()
            );
        } else if (path != null && path.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CREDENTIALS_PATH.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentialFilePath.getMessage());
        } else if (credentialsString != null && credentialsString.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CREDENTIALS_STRING.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyCredentialsString.getMessage());
        } else if (token != null && token.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_TOKEN_VALUE.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyToken.getMessage());
        } else if (apiKey != null) {
            if (apiKey.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_API_KEY_VALUE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyApikey.getMessage());
            } else {
                Pattern pattern = Pattern.compile(Constants.API_KEY_REGEX);
                Matcher matcher = pattern.matcher(apiKey);
                if (!matcher.matches()) {
                    LogUtil.printErrorLog(ErrorLogs.INVALID_API_KEY.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidApikey.getMessage());
                }
            }
        } else if (roles != null) {
            if (roles.isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_ROLES.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRoles.getMessage());
            } else {
                for (int index = 0; index < roles.size(); index++) {
                    String role = roles.get(index);
                    if (role == null || role.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_ROLE_IN_ROLES.getLog(), Integer.toString(index)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRoleInRoles.getMessage());
                    }
                }
            }
        }
        if (context != null) {
            if (context instanceof String) {
                String ctxStr = (String) context;
                if (ctxStr.trim().isEmpty()) {
                    LogUtil.printErrorLog(ErrorLogs.EMPTY_OR_NULL_CONTEXT.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyContext.getMessage());
                }
            } else if (context instanceof Map) {
                Map<?, ?> ctxMap = (Map<?, ?>) context;
                if (ctxMap.isEmpty()) {
                    LogUtil.printErrorLog(ErrorLogs.EMPTY_OR_NULL_CONTEXT.getLog());
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyContext.getMessage());
                }
                Pattern ctxKeyPattern = Pattern.compile(Constants.CONTEXT_KEY_REGEX);
                for (Object key : ctxMap.keySet()) {
                    if (key == null || !ctxKeyPattern.matcher(key.toString()).matches()) {
                        String keyStr = key == null ? "null" : key.toString();
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.INVALID_CONTEXT_MAP_KEY.getLog(), keyStr));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                                Utils.parameterizedString(ErrorMessage.InvalidContextMapKey.getMessage(), keyStr));
                    }
                }
            } else {
                LogUtil.printErrorLog(ErrorLogs.INVALID_CONTEXT_TYPE.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidContextType.getMessage());
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
        ArrayList<DetokenizeData> tokens = request.getDetokenizeData();
        if (tokens == null || tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_DETOKENIZE_DATA.getLog(), InterfaceName.DETOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyDetokenizeData.getMessage());
        }

        for (int index = 0; index < tokens.size(); index++) {
            DetokenizeData token = tokens.get(index);
            if (token == null || token.getToken() == null || token.getToken().trim().isEmpty()) {
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

    public static void validateDeleteTokensRequest(DeleteTokensRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.DELETE_TOKENS_REQUEST_NULL.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.DeleteTokensRequestNull.getMessage());
        }
        List<String> tokens = request.getTokens();
        if (tokens == null || tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_DELETE_TOKENS_DATA.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyDeleteTokensData.getMessage());
        }

        for (int index = 0; index < tokens.size(); index++) {
            String token = tokens.get(index);
            if (token == null || token.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_TOKEN_IN_DELETE_TOKENS_DATA.getLog(),
                        InterfaceName.DELETE.getName(),
                        String.valueOf(index)));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenInDeleteTokensData.getMessage());
            }
        }
    }

    public static void validateTokenizeRequest(TokenizeRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TOKENIZE_REQUEST_NULL.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokenizeRequestNull.getMessage());
        }
        ArrayList<TokenizeRecord> data = request.getData();
        if (data == null || data.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENIZE_DATA.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenizeData.getMessage());
        }
        for (int index = 0; index < data.size(); index++) {
            TokenizeRecord record = data.get(index);
            if (record == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.TOKENIZE_RECORD_NULL.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokenizeRecordNull.getMessage());
            }
            Object value = record.getValue();
            boolean isInvalidValue = value == null || (value instanceof String && ((String) value).trim().isEmpty());
            if (isInvalidValue) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_VALUE_IN_TOKENIZE_RECORD.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInTokenizeRecord.getMessage());
            }
            List<String> tokenGroupNames = record.getTokenGroupNames();
            if (tokenGroupNames == null || tokenGroupNames.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_TOKEN_GROUP_NAMES_IN_TOKENIZE_RECORD.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenGroupNamesInTokenizeRecord.getMessage());
            }
            for (int groupIndex = 0; groupIndex < tokenGroupNames.size(); groupIndex++) {
                String groupName = tokenGroupNames.get(groupIndex);
                if (groupName == null || groupName.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_TOKEN_GROUP_NAME_IN_TOKENIZE_RECORD.getLog(),
                            InterfaceName.TOKENIZE.getName(),
                            String.valueOf(groupIndex)));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenGroupNameInTokenizeRecord.getMessage());
                }
            }
        }
    }

}
