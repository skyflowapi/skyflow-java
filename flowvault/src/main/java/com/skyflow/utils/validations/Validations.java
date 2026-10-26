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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        if (vaultConfig == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_CONFIG_IS_NULL.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullVaultConfig.getMessage());
        }
        String vaultId = vaultConfig.getVaultId();
        String clusterId = vaultConfig.getClusterId();
        String vaultUrl = vaultConfig.getVaultUrl();
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

        if (vaultUrl != null) {
            if (vaultUrl.trim().isEmpty()) {
                LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_URL.getLog());
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultUrl.getMessage());
            }
            // TEMP: vault URL format validation (https-only) disabled for local mock-server testing.
            // else if (!Utils.isValidUrl(vaultUrl)) {
            //     LogUtil.printErrorLog(ErrorLogs.INVALID_VAULT_URL_FORMAT.getLog());
            //     throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultUrlFormat.getMessage());
            // }
        } else if (Utils.getEnvVaultUrl() == null) {
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
        if (insertRequest == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.INSERT_REQUEST_NULL.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InsertRequestNull.getMessage());
        }
        List<InsertRequestRecord> records = insertRequest.getRecords();
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

        for (InsertRequestRecord record : records) {
            if (record == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INVALID_RECORD.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidRecord.getMessage());
            }
            validateUpsertOptions(record.getUpsert());
        }
        validateUpsertOptions(insertRequest.getUpsert());
        validateTableAndUpsertPlacement(insertRequest, records);

        for (InsertRequestRecord record : records) {
            if (record.getData() != null) {
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
            validateInsertRecordTokens(record.getTokens());
        }
    }

    // Tokens are optional on an insert record, but when supplied the map must not be empty and
    // every entry must have a non-blank key and value — mirroring the checks on data above.
    private static void validateInsertRecordTokens(Map<String, Object> tokens) throws SkyflowException {
        if (tokens == null) {
            return;
        }
        if (tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENS.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokens.getMessage());
        }
        for (String key : tokens.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_KEY_IN_TOKENS.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInTokens.getMessage());
            }
            Object value = tokens.get(key);
            if (value == null || value.toString().trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_VALUE_IN_TOKENS.getLog(),
                        InterfaceName.INSERT.getName(), key
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInTokens.getMessage());
            }
        }
    }

    /**
     * Table name must live at exactly one level — either on the request, or on every record.
     * Upsert is optional, but wherever it is supplied it must sit at the same level as the
     * table name; it need not appear on every record.
     */
    private static void validateTableAndUpsertPlacement(
            InsertRequest insertRequest, List<InsertRequestRecord> records) throws SkyflowException {
        boolean tableAtRequest = hasText(insertRequest.getTableName());
        boolean upsertAtRequest = insertRequest.getUpsert() != null;

        int recordsWithTable = 0;
        boolean upsertAtRecords = false;
        for (InsertRequestRecord record : records) {
            if (hasText(record.getTableName())) recordsWithTable++;
            if (record.getUpsert() != null) upsertAtRecords = true;
        }
        boolean tableAtRecords = recordsWithTable > 0;

        // ── table name: exactly one level ────────────────────────────────────
        if (tableAtRequest && tableAtRecords) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.TableSpecifiedInRequestAndRecordObject.getMessage());
        }
        if (!tableAtRequest && recordsWithTable != records.size()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_NOT_SPECIFIED_AT_BOTH_PLACE.getLog(), InterfaceName.INSERT.getName()));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.TableNotSpecifiedInRequestAndRecordObject.getMessage());
        }

        // ── upsert (optional) must match the table name's level ──────────────
        if (upsertAtRecords && !tableAtRecords) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.UPSERT_TABLE_REQUEST_AT_RECORD_LEVEL.getLog(), InterfaceName.INSERT.getName()));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.UpsertTableRequestAtRecordLevel.getMessage());
        }
        if (upsertAtRequest && !tableAtRequest) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.UPSERT_TABLE_REQUEST_AT_REQUEST_LEVEL.getLog(), InterfaceName.INSERT.getName()));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.UpsertTableRequestAtRequestLevel.getMessage());
        }
    }

    private static boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private static void validateUpsertOptions(UpsertOptions upsert) throws SkyflowException {
        if (upsert == null) {
            return;
        }
        if (upsert.getUniqueColumns() == null || upsert.getUniqueColumns().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_UPSERT_VALUES.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUpsertValues.getMessage());
        }
        // updateType is a free-form String on the request, but only the wire enum's values reach
        // the wire. Reject anything else here rather than silently dropping it during mapping.
        String updateType = upsert.getUpdateType();
        if (updateType != null && !isKnownUpdateType(updateType)) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.INVALID_UPSERT_UPDATE_TYPE.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.InvalidUpsertUpdateType.getMessage());
        }
    }

    private static boolean isKnownUpdateType(String updateType) {
        return Utils.toUpsertUpdateType(updateType) != null;
    }

    public static void validateDetokenizeRequest(DetokenizeRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.DETOKENIZE_REQUEST_NULL.getLog(), InterfaceName.DETOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.DetokenizeRequestNull.getMessage());
        }
        List<String> tokens = request.getTokens();
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

    // ── Bulk (batched/concurrent) request validations ────────────────────────

    // BulkInsertRequest is an InsertRequest with no extra state, so the field rules are identical.
    // The one addition: records must be BulkInsertRequestRecord, since BulkInsertResponse hands
    // them back as such from getRecordsToRetry(). `records` is typed to the parent (it is
    // inherited), so this is enforced here rather than by the compiler.
    // The service accepts at most Constants.MAX_BULK_DATA_SIZE items per bulk call; batching
    // splits the payload but does not lift that ceiling.
    private static void validateBulkDataSize(
            int size, ErrorLogs log, ErrorMessage message, InterfaceName interfaceName) throws SkyflowException {
        if (size > Constants.MAX_BULK_DATA_SIZE) {
            LogUtil.printErrorLog(Utils.parameterizedString(log.getLog(), interfaceName.getName()));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), message.getMessage());
        }
    }

    public static void validateBulkInsertRequest(BulkInsertRequest insertRequest) throws SkyflowException {
        validateInsertRequest(insertRequest);
        validateBulkDataSize(insertRequest.getRecords().size(), ErrorLogs.RECORD_SIZE_EXCEED,
                ErrorMessage.RecordSizeExceedError, InterfaceName.INSERT);

        for (InsertRequestRecord record : insertRequest.getRecords()) {
            if (!(record instanceof BulkInsertRequestRecord)) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INVALID_RECORD.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidRecord.getMessage());
            }
        }
    }

    // BulkDetokenizeRequest is a DetokenizeRequest with no extra state, so the rules are identical.
    public static void validateBulkDetokenizeRequest(BulkDetokenizeRequest request) throws SkyflowException {
        validateDetokenizeRequest(request);
        validateBulkDataSize(request.getTokens().size(), ErrorLogs.TOKENS_SIZE_EXCEED,
                ErrorMessage.TokensSizeExceedError, InterfaceName.DETOKENIZE);
    }

    public static void validateDeleteRequest(DeleteRequest deleteRequest) throws SkyflowException {
        if (deleteRequest == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.DELETE_REQUEST_NULL.getLog(), InterfaceName.DELETE_RECORDS.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.DeleteRequestNull.getMessage());
        }
        validateTableRequired(deleteRequest.getTableName(), InterfaceName.DELETE_RECORDS);
        validateIdsOrUniqueValues(deleteRequest.getSkyflowIds(), deleteRequest.getUniqueValues(), InterfaceName.DELETE_RECORDS);
    }

    public static void validateBulkDeleteTokensRequest(BulkDeleteTokensRequest request) throws SkyflowException {
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
        validateBulkDataSize(tokens.size(), ErrorLogs.DELETE_TOKENS_SIZE_EXCEED,
                ErrorMessage.DeleteTokensSizeExceedError, InterfaceName.DELETE);

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

    public static void validateUpdateRequest(UpdateRequest updateRequest) throws SkyflowException {
        if (updateRequest == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.UPDATE_REQUEST_NULL.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.UpdateRequestNull.getMessage());
        }
        String tableName = updateRequest.getTableName();
        if (tableName == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (tableName.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        }

        List<UpdateRequestRecord> records = updateRequest.getRecords();
        if (records == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.RECORDS_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RecordsKeyError.getMessage());
        } else if (records.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_RECORDS.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRecords.getMessage());
        }

        for (UpdateRequestRecord record : records) {
            if (record == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INVALID_RECORD.getLog(), InterfaceName.UPDATE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.UpdateRecordNull.getMessage());
            }
            String skyflowId = record.getSkyflowId();
            if (skyflowId == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.SKYFLOW_ID_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RecordSkyflowIdKeyError.getMessage());
            } else if (skyflowId.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_SKYFLOW_ID.getLog(), InterfaceName.UPDATE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptySkyflowIdInRecord.getMessage());
            }

            if (record.getData() != null) {
                for (String key : record.getData().keySet()) {
                    if (key == null || key.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.UPDATE.getName()
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInRecords.getMessage());
                    } else {
                        Object value = record.getData().get(key);
                        if (value == null || value.toString().trim().isEmpty()) {
                            LogUtil.printErrorLog(Utils.parameterizedString(
                                    ErrorLogs.EMPTY_OR_NULL_VALUE_IN_VALUES.getLog(),
                                    InterfaceName.UPDATE.getName(), key
                            ));
                            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInValues.getMessage());
                        }
                    }
                }
            }

            Map<String, Object> tokens = record.getTokens();
            if (tokens != null) {
                if (tokens.isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_TOKENS.getLog(), InterfaceName.UPDATE.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokens.getMessage());
                }
                for (String key : tokens.keySet()) {
                    if (key == null || key.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_KEY_IN_TOKENS.getLog(), InterfaceName.UPDATE.getName()
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInTokens.getMessage());
                    }
                    Object value = tokens.get(key);
                    if (value == null || value.toString().trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_VALUE_IN_TOKENS.getLog(),
                                InterfaceName.UPDATE.getName(), key
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInTokens.getMessage());
                    }
                }
            }
        }
    }

    public static void validateGetRequest(GetRequest getRequest) throws SkyflowException {
        if (getRequest == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.GET_REQUEST_NULL.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.GetRequestNull.getMessage());
        }

        boolean hasSingleTableFields = hasText(getRequest.getTableName())
                || (getRequest.getSkyflowIds() != null && !getRequest.getSkyflowIds().isEmpty())
                || (getRequest.getColumns() != null && !getRequest.getColumns().isEmpty())
                || (getRequest.getUniqueValues() != null && !getRequest.getUniqueValues().isEmpty())
                || (getRequest.getColumnRedactions() != null && !getRequest.getColumnRedactions().isEmpty());
        boolean hasRecords = getRequest.getRecords() != null && !getRequest.getRecords().isEmpty();

        if (hasSingleTableFields && hasRecords) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.BOTH_SINGLE_TABLE_FIELDS_AND_RECORDS_PASSED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.BothSingleTableFieldsAndRecordsSpecified.getMessage());
        }

        if (hasRecords) {
            for (GetRequestRecord record : getRequest.getRecords()) {
                if (record == null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.NULL_GET_RECORD_REQUEST_OBJECT.getLog(), InterfaceName.GET.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullGetRecordRequest.getMessage());
                }
                validateTableRequired(record.getTableName(), InterfaceName.GET);
                validateIdsOrUniqueValues(record.getSkyflowIds(), record.getUniqueValues(), InterfaceName.GET);
                validateFields(record.getColumns());
                validateColumnRedactions(record.getColumnRedactions());
            }
            return;
        }

        validateTableRequired(getRequest.getTableName(), InterfaceName.GET);
        validateIdsOrUniqueValues(getRequest.getSkyflowIds(), getRequest.getUniqueValues(), InterfaceName.GET);
        validateFields(getRequest.getColumns());
        validateColumnRedactions(getRequest.getColumnRedactions());
    }

    private static void validateTableRequired(String table, InterfaceName interfaceName) throws SkyflowException {
        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), interfaceName.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), interfaceName.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        }
    }

    // Either ids or uniqueValues selects the records within the table; specifying both, or
    // neither, is invalid.
    private static void validateIdsOrUniqueValues(List<String> ids, List<Map<String, Object>> uniqueValues, InterfaceName interfaceName) throws SkyflowException {
        boolean hasIds = ids != null && !ids.isEmpty();
        boolean hasUniqueValues = uniqueValues != null && !uniqueValues.isEmpty();

        if (hasIds && hasUniqueValues) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.BOTH_IDS_AND_UNIQUE_VALUES_PASSED.getLog(), interfaceName.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.BothIdsAndUniqueValuesSpecified.getMessage());
        }
        if (!hasIds && !hasUniqueValues) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.NEITHER_IDS_NOR_UNIQUE_VALUES_PASSED.getLog(), interfaceName.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.IdsOrUniqueValuesKeyError.getMessage());
        }

        if (hasIds) {
            for (int index = 0; index < ids.size(); index++) {
                String id = ids.get(index);
                if (id == null || id.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_ID_IN_IDS.getLog(), interfaceName.getName(), String.valueOf(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyIdInIds.getMessage());
                }
            }
        } else {
            for (int index = 0; index < uniqueValues.size(); index++) {
                Map<String, Object> uniqueValue = uniqueValues.get(index);
                if (uniqueValue == null || uniqueValue.isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_UNIQUE_VALUE_IN_UNIQUE_VALUES.getLog(), interfaceName.getName(), String.valueOf(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUniqueValueInUniqueValues.getMessage());
                }
            }
        }
    }

    // fields is optional; when supplied, it must not be empty and no entry may be null/blank.
    private static void validateFields(List<String> fields) throws SkyflowException {
        if (fields == null) {
            return;
        }
        if (fields.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_FIELDS.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyFields.getMessage());
        }
        for (int index = 0; index < fields.size(); index++) {
            String field = fields.get(index);
            if (field == null || field.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_FIELD_IN_FIELDS.getLog(), InterfaceName.GET.getName(), String.valueOf(index)
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyFieldInFields.getMessage());
            }
        }
    }

    // columnRedactions is optional; when supplied, no entry may be null and each must carry a
    // non-blank columnName and redaction.
    private static void validateColumnRedactions(List<ColumnRedactions> columnRedactions) throws SkyflowException {
        if (columnRedactions == null) {
            return;
        }
        for (ColumnRedactions columnRedaction : columnRedactions) {
            if (columnRedaction == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.NULL_COLUMN_REDACTION_OBJECT.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullColumnRedactions.getMessage());
            }
            String columnName = columnRedaction.getColumnName();
            if (columnName == null || columnName.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.NULL_COLUMN_NAME_IN_COLUMN_REDACTION.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullColumnNameInColumnRedaction.getMessage());
            }
            String redaction = columnRedaction.getRedaction();
            if (redaction == null || redaction.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_REDACTION_IN_COLUMN_REDACTION.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.NullRedactionInColumnRedaction.getMessage());
            }
        }
    }

    public static void validateBulkTokenizeRequest(BulkTokenizeRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TOKENIZE_REQUEST_NULL.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokenizeRequestNull.getMessage());
        }
        List<BulkTokenizeRequestRecord> records = request.getRecords();
        if (records == null || records.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENIZE_DATA.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenizeData.getMessage());
        }
        validateBulkDataSize(records.size(), ErrorLogs.TOKENIZE_DATA_SIZE_EXCEED,
                ErrorMessage.TokenizeDataSizeExceedError, InterfaceName.TOKENIZE);

        for (int i = 0; i < records.size(); i++) {
            BulkTokenizeRequestRecord record;
            try {
                record = records.get(i);
            } catch (ClassCastException e) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INVALID_BULK_TOKENIZE_RECORD_TYPE.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidBulkTokenizeRecordType.getMessage());
            }
            if (record == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.TOKENIZE_RECORD_NULL.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokenizeRecordNull.getMessage());
            }
            Object value = record.getValue();
            boolean isInvalid = value == null
                    || (value instanceof String && ((String) value).trim().isEmpty());
            if (isInvalid) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_VALUE_IN_TOKENIZE_RECORD.getLog(), InterfaceName.TOKENIZE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInTokenizeRecord.getMessage());
            }
            List<String> tokenGroupNames = record.getTokenGroupNames();
            if (tokenGroupNames != null) {
                for (int j = 0; j < tokenGroupNames.size(); j++) {
                    String groupName = tokenGroupNames.get(j);
                    if (groupName == null || groupName.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_TOKEN_GROUP_NAME_IN_TOKENIZE_RECORD.getLog(),
                                InterfaceName.TOKENIZE.getName(),
                                String.valueOf(j)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenGroupNameInTokenizeRecord.getMessage());
                    }
                }
            }
        }
    }

    public static void validateQueryRequest(QueryRequest queryRequest) throws SkyflowException {
        if (queryRequest == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.QUERY_REQUEST_NULL.getLog(), InterfaceName.QUERY.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.QueryRequestNull.getMessage());
        }
        String query = queryRequest.getQuery();
        if (query == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.QUERY_IS_REQUIRED.getLog(), InterfaceName.QUERY.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.QueryKeyError.getMessage());
        } else if (query.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_QUERY.getLog(), InterfaceName.QUERY.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyQuery.getMessage());
        }
    }

    public static void validateGetTokensRequest(GetTokensRequest request) throws SkyflowException {
        if (request == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.GET_TOKENS_REQUEST_NULL.getLog(), InterfaceName.GET_TOKENS.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.GetTokensRequestNull.getMessage());
        }
        List<GetTokensRequestRecord> records = request.getRecords();
        if (records == null || records.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_GET_TOKENS_RECORDS.getLog(), InterfaceName.GET_TOKENS.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyGetTokensRecords.getMessage());
        }
        for (int i = 0; i < records.size(); i++) {
            GetTokensRequestRecord record = records.get(i);
            if (record == null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.GET_TOKENS_RECORD_NULL.getLog(), InterfaceName.GET_TOKENS.getName(), String.valueOf(i)
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.GetTokensRecordNull.getMessage());
            }
            Object value = record.getValue();
            if (value == null || (value instanceof String && ((String) value).trim().isEmpty())) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_VALUE_IN_GET_TOKENS_RECORD.getLog(), InterfaceName.GET_TOKENS.getName(), String.valueOf(i)
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInGetTokensRecord.getMessage());
            }
            if (!hasText(record.getTokenGroupName())) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_TOKEN_GROUP_NAME_IN_GET_TOKENS_RECORD.getLog(), InterfaceName.GET_TOKENS.getName(), String.valueOf(i)
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenGroupNameInGetTokensRecord.getMessage());
            }
        }
    }

    public static void validateUploadFilesRequest(UploadFilesRequest request) throws SkyflowException {
        InterfaceName upload = InterfaceName.UPLOAD_FILES;
        if (request == null) {
            throw invalid(ErrorLogs.UPLOAD_FILES_REQUEST_NULL, ErrorMessage.UploadFilesRequestNull, upload, null);
        }
        List<UploadFilesRequestRecord> records = request.getRecords();
        if (records == null || records.isEmpty()) {
            throw invalid(ErrorLogs.EMPTY_UPLOAD_FILES_RECORDS, ErrorMessage.EmptyUploadFilesRecords, upload, null);
        }
        for (int i = 0; i < records.size(); i++) {
            UploadFilesRequestRecord record = records.get(i);
            if (record == null) {
                throw invalid(ErrorLogs.UPLOAD_FILES_RECORD_NULL, ErrorMessage.UploadFilesRecordNull, upload, i);
            }
            if (!hasText(record.getTableName())) {
                throw invalid(ErrorLogs.EMPTY_TABLE_NAME_IN_UPLOAD_FILES_RECORD,
                        ErrorMessage.EmptyTableNameInUploadFilesRecord, upload, i);
            }
            // Omitting skyflowId creates a new record, so a blank one is rejected rather than
            // silently treated as absent.
            if (record.getSkyflowId() != null && !hasText(record.getSkyflowId())) {
                throw invalid(ErrorLogs.EMPTY_SKYFLOW_ID_IN_UPLOAD_FILES_RECORD,
                        ErrorMessage.EmptySkyflowIdInUploadFilesRecord, upload, i);
            }
            List<UploadFilesRequestColumn> columns = record.getColumns();
            if (columns == null || columns.isEmpty()) {
                throw invalid(ErrorLogs.EMPTY_UPLOAD_FILES_COLUMNS, ErrorMessage.EmptyUploadFilesColumns, upload, i);
            }
            // Signed URLs come back keyed by column name, so a repeated column would overwrite
            // the other file under the same URL.
            Set<String> seen = new HashSet<>();
            for (UploadFilesRequestColumn column : columns) {
                validateUploadFilesColumn(column, i);
                if (!seen.add(column.getColumn())) {
                    throw invalid(ErrorLogs.DUPLICATE_COLUMN_IN_UPLOAD_FILES_RECORD,
                            ErrorMessage.DuplicateColumnInUploadFilesRecord, upload, i);
                }
            }
        }
    }

    // Exactly one file source per column (filePath, base64 or fileObject), and it must be usable.
    private static void validateUploadFilesColumn(UploadFilesRequestColumn column, int recordIndex)
            throws SkyflowException {
        InterfaceName upload = InterfaceName.UPLOAD_FILES;
        if (column == null) {
            throw invalid(ErrorLogs.UPLOAD_FILES_COLUMN_NULL, ErrorMessage.UploadFilesColumnNull, upload, recordIndex);
        }
        if (!hasText(column.getColumn())) {
            throw invalid(ErrorLogs.EMPTY_COLUMN_IN_UPLOAD_FILES_COLUMN,
                    ErrorMessage.EmptyColumnInUploadFilesColumn, upload, recordIndex);
        }
        int sources = (hasText(column.getFilePath()) ? 1 : 0) + (hasText(column.getBase64()) ? 1 : 0)
                + (column.getFileObject() != null ? 1 : 0);
        if (sources == 0) {
            throw invalid(ErrorLogs.MISSING_FILE_SOURCE_IN_UPLOAD_FILES_COLUMN,
                    ErrorMessage.MissingFileSourceInUploadFilesColumn, upload, recordIndex);
        }
        if (sources > 1) {
            throw invalid(ErrorLogs.MULTIPLE_FILE_SOURCES_IN_UPLOAD_FILES_COLUMN,
                    ErrorMessage.MultipleFileSourcesInUploadFilesColumn, upload, recordIndex);
        }
        if (hasText(column.getFilePath())) {
            File file = new File(column.getFilePath());
            if (!file.isFile() || !file.canRead()) {
                throw invalid(ErrorLogs.INVALID_FILE_PATH_IN_UPLOAD_FILES_COLUMN,
                        ErrorMessage.InvalidFilePathInUploadFilesColumn, upload, recordIndex);
            }
        } else if (hasText(column.getBase64())) {
            if (!hasText(column.getFileName())) {
                throw invalid(ErrorLogs.FILE_NAME_REQUIRED_WITH_BASE64_IN_UPLOAD_FILES_COLUMN,
                        ErrorMessage.FileNameRequiredWithBase64InUploadFilesColumn, upload, recordIndex);
            }
        } else if (!column.getFileObject().isFile() || !column.getFileObject().canRead()) {
            throw invalid(ErrorLogs.INVALID_FILE_OBJECT_IN_UPLOAD_FILES_COLUMN,
                    ErrorMessage.InvalidFileObjectInUploadFilesColumn, upload, recordIndex);
        }
    }

    public static void validateDeleteFilesRequest(DeleteFilesRequest request) throws SkyflowException {
        InterfaceName delete = InterfaceName.DELETE_FILES;
        if (request == null) {
            throw invalid(ErrorLogs.DELETE_FILES_REQUEST_NULL, ErrorMessage.DeleteFilesRequestNull, delete, null);
        }
        List<DeleteFilesRequestRecord> records = request.getRecords();
        if (records == null || records.isEmpty()) {
            throw invalid(ErrorLogs.EMPTY_DELETE_FILES_RECORDS, ErrorMessage.EmptyDeleteFilesRecords, delete, null);
        }
        for (int i = 0; i < records.size(); i++) {
            DeleteFilesRequestRecord record = records.get(i);
            if (record == null) {
                throw invalid(ErrorLogs.DELETE_FILES_RECORD_NULL, ErrorMessage.DeleteFilesRecordNull, delete, i);
            }
            if (!hasText(record.getTableName())) {
                throw invalid(ErrorLogs.EMPTY_TABLE_NAME_IN_DELETE_FILES_RECORD,
                        ErrorMessage.EmptyTableNameInDeleteFilesRecord, delete, i);
            }
            List<String> columns = record.getColumns();
            if (columns == null || columns.isEmpty()) {
                throw invalid(ErrorLogs.EMPTY_DELETE_FILES_COLUMNS, ErrorMessage.EmptyDeleteFilesColumns, delete, i);
            }
            Set<String> seen = new HashSet<>();
            for (String column : columns) {
                if (!hasText(column)) {
                    throw invalid(ErrorLogs.EMPTY_COLUMN_IN_DELETE_FILES_COLUMNS,
                            ErrorMessage.EmptyColumnInDeleteFilesColumns, delete, i);
                }
                if (!seen.add(column)) {
                    throw invalid(ErrorLogs.DUPLICATE_COLUMN_IN_DELETE_FILES_COLUMNS,
                            ErrorMessage.DuplicateColumnInDeleteFilesColumns, delete, i);
                }
            }
            List<Map<String, Object>> uniqueValues = record.getUniqueValues();
            boolean hasSkyflowId = hasText(record.getSkyflowId());
            boolean hasUniqueValues = uniqueValues != null && !uniqueValues.isEmpty();
            if (hasSkyflowId == hasUniqueValues) {
                throw invalid(ErrorLogs.INVALID_ID_OR_UNIQUE_VALUES_IN_DELETE_FILES_RECORD,
                        ErrorMessage.InvalidIdOrUniqueValuesInDeleteFilesRecord, delete, i);
            }
            // Same per-entry rule as Get and Delete records.
            if (hasUniqueValues) {
                for (int index = 0; index < uniqueValues.size(); index++) {
                    Map<String, Object> uniqueValue = uniqueValues.get(index);
                    if (uniqueValue == null || uniqueValue.isEmpty()) {
                        throw invalid(ErrorLogs.EMPTY_OR_NULL_UNIQUE_VALUE_IN_UNIQUE_VALUES,
                                ErrorMessage.EmptyUniqueValueInUniqueValues, delete, index);
                    }
                }
            }
        }
    }

    /** Logs a validation failure and builds the exception to throw for it. */
    private static SkyflowException invalid(ErrorLogs log, ErrorMessage message, InterfaceName interfaceName,
                                            Integer index) {
        LogUtil.printErrorLog(index == null
                ? Utils.parameterizedString(log.getLog(), interfaceName.getName())
                : Utils.parameterizedString(log.getLog(), interfaceName.getName(), String.valueOf(index)));
        return new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), message.getMessage());
    }
}
