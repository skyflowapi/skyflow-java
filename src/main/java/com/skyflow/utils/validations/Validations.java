package com.skyflow.utils.validations;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.InterfaceName;
import com.skyflow.enums.RedactionType;
import com.skyflow.enums.TokenMode;
import com.skyflow.errors.ErrorCode;
import com.skyflow.errors.ErrorMessage;
import com.skyflow.errors.SkyflowException;
import com.skyflow.logs.ErrorLogs;
import com.skyflow.utils.Constants;
import com.skyflow.utils.Utils;
import com.skyflow.utils.logger.LogUtil;
import com.skyflow.vault.connection.InvokeConnectionRequest;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.ColumnValue;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validations {
    private Validations() {
    }

    public static void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        String vaultId = vaultConfig.getVaultId();
        String clusterId = vaultConfig.getClusterId();
        Credentials credentials = vaultConfig.getCredentials();
        if (vaultId == null) {
            LogUtil.printErrorLog(ErrorLogs.VAULT_ID_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidVaultId.getMessage());
        } else if (vaultId.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_VAULT_ID.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyVaultId.getMessage());
        } else if (clusterId == null) {
            LogUtil.printErrorLog(ErrorLogs.CLUSTER_ID_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidClusterId.getMessage());
        } else if (clusterId.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CLUSTER_ID.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyClusterId.getMessage());
        } else if (credentials != null) {
            validateCredentials(credentials);
        }
    }

    public static void validateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
        String connectionId = connectionConfig.getConnectionId();
        String connectionUrl = connectionConfig.getConnectionUrl();

        if (connectionId == null) {
            LogUtil.printErrorLog(ErrorLogs.CONNECTION_ID_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidConnectionId.getMessage());
        } else if (connectionId.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CONNECTION_ID.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyConnectionId.getMessage());
        } else if (connectionUrl == null) {
            LogUtil.printErrorLog(ErrorLogs.CONNECTION_URL_IS_REQUIRED.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidConnectionUrl.getMessage());
        } else if (connectionUrl.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_CONNECTION_URL.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyConnectionUrl.getMessage());
        } else if (isInvalidURL(connectionUrl)) {
            LogUtil.printErrorLog(ErrorLogs.INVALID_CONNECTION_URL.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidConnectionUrlFormat.getMessage());
        }
    }

    public static void validateInvokeConnectionRequest(InvokeConnectionRequest invokeConnectionRequest) throws SkyflowException {
        Map<String, String> requestHeaders = invokeConnectionRequest.getRequestHeaders();
        Map<String, String> pathParams = invokeConnectionRequest.getPathParams();
        Map<String, String> queryParams = invokeConnectionRequest.getQueryParams();
        Object requestBody = invokeConnectionRequest.getRequestBody();

        if (requestHeaders != null) {
            if (requestHeaders.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_REQUEST_HEADERS.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRequestHeaders.getMessage());
            } else {
                for (String header : requestHeaders.keySet()) {
                    String headerValue = requestHeaders.get(header);
                    if (header == null || header.trim().isEmpty() || headerValue == null || headerValue.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.INVALID_REQUEST_HEADERS.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidRequestHeaders.getMessage());
                    }
                }
            }
        }

        if (pathParams != null) {
            if (pathParams.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_PATH_PARAMS.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyPathParams.getMessage());
            } else {
                for (String param : pathParams.keySet()) {
                    String paramValue = pathParams.get(param);
                    if (param == null || param.trim().isEmpty() || paramValue == null || paramValue.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.INVALID_PATH_PARAM.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidPathParams.getMessage());
                    }
                }
            }
        }

        if (queryParams != null) {
            if (queryParams.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_QUERY_PARAMS.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyQueryParams.getMessage());
            } else {
                for (String param : queryParams.keySet()) {
                    String paramValue = queryParams.get(param);
                    if (param == null || param.trim().isEmpty() || paramValue == null || paramValue.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.INVALID_QUERY_PARAM.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidQueryParams.getMessage());
                    }
                }
            }
        }

        if (requestBody != null) {
            Map<String, String> requestBodyMap = (Map<String, String>) requestBody;
            if (requestBodyMap.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_REQUEST_BODY.getLog(), InterfaceName.INVOKE_CONNECTION.getName()));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyRequestBody.getMessage());
            }
        }
    }

    public static void validateCredentials(Credentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        String path = credentials.getPath();
        String credentialsString = credentials.getCredentialsString();
        String token = credentials.getToken();
        String apiKey = credentials.getApiKey();
        String context = credentials.getContext();
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
        if (context != null && context.trim().isEmpty()) {
            LogUtil.printErrorLog(ErrorLogs.EMPTY_OR_NULL_CONTEXT.getLog());
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyContext.getMessage());
        }
    }

    public static void validateDetokenizeRequest(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        ArrayList<String> tokens = detokenizeRequest.getTokens();
        if (tokens == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TOKENS_REQUIRED.getLog(), InterfaceName.DETOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidDataTokens.getMessage());
        } else if (tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENS.getLog(), InterfaceName.DETOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyDataTokens.getMessage());
        } else {
            for (int index = 0; index < tokens.size(); index++) {
                String token = tokens.get(index);
                if (token == null || token.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_TOKEN_IN_TOKENS.getLog(),
                            InterfaceName.DETOKENIZE.getName(), Integer.toString(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokenInDataTokens.getMessage());
                }
            }
        }
    }

    public static void validateInsertRequest(InsertRequest insertRequest) throws SkyflowException {
        String table = insertRequest.getTable();
        ArrayList<HashMap<String, Object>> values = insertRequest.getValues();
        ArrayList<HashMap<String, Object>> tokens = insertRequest.getTokens();
        String upsert = insertRequest.getUpsert();
        Boolean homogeneous = insertRequest.getHomogeneous();
        TokenMode tokenMode = insertRequest.getTokenMode();

        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        } else if (values == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.VALUES_IS_REQUIRED.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ValuesKeyError.getMessage());
        } else if (values.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_VALUES.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValues.getMessage());
        } else if (upsert != null) {
            if (upsert.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_UPSERT.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyUpsert.getMessage());
            } else if (homogeneous != null && homogeneous) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.HOMOGENOUS_NOT_SUPPORTED_WITH_UPSERT.getLog(), InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(
                        ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.HomogenousNotSupportedWithUpsert.getMessage()
                );
            }
        }

        for (HashMap<String, Object> valuesMap : values) {
            for (String key : valuesMap.keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInValues.getMessage());
                } else {
                    Object value = valuesMap.get(key);
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

        switch (tokenMode) {
            case DISABLE:
                if (tokens != null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_NOT_ALLOWED_WITH_TOKEN_MODE_DISABLE.getLog(), InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TokensPassedForTokenModeDisable.getMessage());
                }
                break;
            case ENABLE:
                if (tokens == null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_REQUIRED_WITH_TOKEN_MODE.getLog(),
                            InterfaceName.INSERT.getName(), TokenMode.ENABLE.toString()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            Utils.parameterizedString(ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE.toString())
                    );
                }
                validateTokensForInsertRequest(tokens, values, tokenMode);
                break;
            case ENABLE_STRICT:
                if (tokens == null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_REQUIRED_WITH_TOKEN_MODE.getLog(),
                            InterfaceName.INSERT.getName(), TokenMode.ENABLE_STRICT.toString()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            Utils.parameterizedString(ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE_STRICT.toString())
                    );
                } else if (tokens.size() != values.size()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.INSUFFICIENT_TOKENS_PASSED_FOR_TOKEN_MODE_ENABLE_STRICT.getLog(),
                            InterfaceName.INSERT.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage()
                    );
                }
                validateTokensForInsertRequest(tokens, values, tokenMode);
                break;
        }
    }

    public static void validateGetRequest(GetRequest getRequest) throws SkyflowException {
        String table = getRequest.getTable();
        ArrayList<String> ids = getRequest.getIds();
        RedactionType redactionType = getRequest.getRedactionType();
        Boolean tokenization = getRequest.getReturnTokens();
        ArrayList<String> fields = getRequest.getFields();
        String offset = getRequest.getOffset();
        String limit = getRequest.getLimit();
        String columnName = getRequest.getColumnName();
        ArrayList<String> columnValues = getRequest.getColumnValues();
        String orderBy = getRequest.getOrderBy();

        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        } else if (ids != null) {
            if (ids.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_IDS.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyIds.getMessage());
            } else {
                for (int index = 0; index < ids.size(); index++) {
                    String id = ids.get(index);
                    if (id == null || id.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_ID_IN_IDS.getLog(),
                                InterfaceName.GET.getName(), Integer.toString(index)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyIdInIds.getMessage());
                    }
                }
            }
        }
        if (fields != null) {
            if (fields.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_FIELDS.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyFields.getMessage());
            } else {
                for (int index = 0; index < fields.size(); index++) {
                    String field = fields.get(index);
                    if (field == null || field.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_FIELD_IN_FIELDS.getLog(),
                                InterfaceName.GET.getName(), Integer.toString(index)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyFieldInFields.getMessage());
                    }
                }
            }
        }
        if (redactionType == null && (tokenization == null || !tokenization)) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.REDACTION_IS_REQUIRED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.RedactionKeyError.getMessage());
        }
        if (tokenization != null && tokenization) {
            if (redactionType != null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.TOKENIZATION_NOT_SUPPORTED_WITH_REDACTION.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.RedactionWithTokensNotSupported.getMessage()
                );
            } else if (columnName != null || columnValues != null) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.TOKENIZATION_SUPPORTED_ONLY_WITH_IDS.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.TokensGetColumnNotSupported.getMessage()
                );
            }
        }
        if (offset != null && offset.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_OFFSET.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyOffset.getMessage());
        }
        if (limit != null && limit.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_LIMIT.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyLimit.getMessage());
        }
        if (ids == null && columnName == null && columnValues == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.NEITHER_IDS_NOR_COLUMN_NAME_PASSED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.UniqueColumnOrIdsKeyError.getMessage());
        } else if (ids != null && (columnName != null || columnValues != null)) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.BOTH_IDS_AND_COLUMN_NAME_PASSED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                    ErrorMessage.BothIdsAndColumnDetailsSpecified.getMessage());
        } else if (columnName == null && columnValues != null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.COLUMN_NAME_IS_REQUIRED.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ColumnNameKeyError.getMessage());
        } else if (columnName != null && columnValues == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.COLUMN_VALUES_IS_REQUIRED_GET.getLog(), InterfaceName.GET.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ColumnValuesKeyErrorGet.getMessage());
        } else if (columnName != null) {
            if (columnName.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_COLUMN_NAME.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyColumnName.getMessage());
            } else if (columnValues.isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_COLUMN_VALUES.getLog(), InterfaceName.GET.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyColumnValues.getMessage());
            } else {
                for (int index = 0; index < columnValues.size(); index++) {
                    String columnValue = columnValues.get(index);
                    if (columnValue == null || columnValue.trim().isEmpty()) {
                        LogUtil.printErrorLog(Utils.parameterizedString(
                                ErrorLogs.EMPTY_OR_NULL_COLUMN_VALUE_IN_COLUMN_VALUES.getLog(),
                                InterfaceName.GET.getName(), Integer.toString(index)
                        ));
                        throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                                ErrorMessage.EmptyValueInColumnValues.getMessage());
                    }
                }
            }
        }
    }

    public static void validateUpdateRequest(UpdateRequest updateRequest) throws SkyflowException {
        String table = updateRequest.getTable();
        HashMap<String, Object> data = updateRequest.getData();
        HashMap<String, Object> tokens = updateRequest.getTokens();
        TokenMode tokenMode = updateRequest.getTokenMode();

        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());
        } else if (data == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.DATA_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.DataKeyError.getMessage());
        } else if (data.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_DATA.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyData.getMessage());
        } else if (!data.containsKey("skyflow_id")) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.SKYFLOW_ID_IS_REQUIRED.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.SkyflowIdKeyError.getMessage());
        } else if (!(data.get("skyflow_id") instanceof String)) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.INVALID_SKYFLOW_ID_TYPE.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.InvalidSkyflowIdType.getMessage());
        } else if (data.get("skyflow_id").toString().trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_SKYFLOW_ID.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptySkyflowId.getMessage());
        } else if (tokens != null && tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENS.getLog(), InterfaceName.UPDATE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokens.getMessage());
        }

        for (String key : data.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_KEY_IN_VALUES.getLog(), InterfaceName.UPDATE.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInValues.getMessage());
            } else {
                Object value = data.get(key);
                if (value == null || value.toString().trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_VALUE_IN_VALUES.getLog(), InterfaceName.UPDATE.getName(), key
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.EmptyValueInValues.getMessage());
                }
            }
        }

        switch (tokenMode) {
            case DISABLE:
                if (tokens != null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_NOT_ALLOWED_WITH_TOKEN_MODE_DISABLE.getLog(), InterfaceName.UPDATE.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.TokensPassedForTokenModeDisable.getMessage());
                }
                break;
            case ENABLE:
                if (tokens == null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_REQUIRED_WITH_TOKEN_MODE.getLog(),
                            InterfaceName.UPDATE.getName(), TokenMode.ENABLE.toString()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                            ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE.toString()));
                }
                validateTokensMapWithTokenStrict(tokens, data);
                break;
            case ENABLE_STRICT:
                if (tokens == null) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.TOKENS_REQUIRED_WITH_TOKEN_MODE.getLog(),
                            InterfaceName.UPDATE.getName(), TokenMode.ENABLE_STRICT.toString()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), Utils.parameterizedString(
                            ErrorMessage.NoTokensWithTokenMode.getMessage(), TokenMode.ENABLE_STRICT.toString()));
                } else if (tokens.size() != (data.size() - 1)) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.INSUFFICIENT_TOKENS_PASSED_FOR_TOKEN_MODE_ENABLE_STRICT.getLog(),
                            InterfaceName.UPDATE.getName()
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage());
                }
                validateTokensMapWithTokenStrict(tokens, data);
                break;
        }
    }

    public static void validateDeleteRequest(DeleteRequest deleteRequest) throws SkyflowException {
        String table = deleteRequest.getTable();
        ArrayList<String> ids = deleteRequest.getIds();
        if (table == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.TABLE_IS_REQUIRED.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.TableKeyError.getMessage());
        } else if (table.trim().isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TABLE_NAME.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTable.getMessage());

        } else if (ids == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.IDS_IS_REQUIRED.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.IdsKeyError.getMessage());
        } else if (ids.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_IDS.getLog(), InterfaceName.DELETE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyIds.getMessage());
        } else {
            for (int index = 0; index < ids.size(); index++) {
                String id = ids.get(index);
                if (id == null || id.trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_ID_IN_IDS.getLog(),
                            InterfaceName.DELETE.getName(), Integer.toString(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyIdInIds.getMessage());
                }
            }
        }
    }

    public static void validateQueryRequest(QueryRequest queryRequest) throws SkyflowException {
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

    public static void validateTokenizeRequest(TokenizeRequest tokenizeRequest) throws SkyflowException {
        List<ColumnValue> columnValues = tokenizeRequest.getColumnValues();

        if (columnValues == null) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.COLUMN_VALUES_IS_REQUIRED_TOKENIZE.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.ColumnValuesKeyErrorTokenize.getMessage());
        } else if (columnValues.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_COLUMN_VALUES.getLog(), InterfaceName.TOKENIZE.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyColumnValues.getMessage());
        } else {
            for (int index = 0; index < columnValues.size(); index++) {
                ColumnValue value = columnValues.get(index);
                if (value.getValue() == null || value.getValue().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_COLUMN_VALUE_IN_COLUMN_VALUES.getLog(),
                            InterfaceName.TOKENIZE.getName(), Integer.toString(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInColumnValues.getMessage());
                } else if (value.getColumnGroup() == null || value.getColumnGroup().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_COLUMN_GROUP_IN_COLUMN_VALUES.getLog(),
                            InterfaceName.TOKENIZE.getName(), Integer.toString(index)
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                            ErrorMessage.EmptyColumnGroupInColumnValue.getMessage());
                }
            }
        }
    }

    private static boolean isInvalidURL(String configURL) {
        try {
            URL url = new URL(configURL);
            if (!url.getProtocol().equals("https")) throw new Exception();
        } catch (Exception e) {
            return true;
        }
        return false;
    }

    private static void validateTokensForInsertRequest(
            ArrayList<HashMap<String, Object>> tokens,
            ArrayList<HashMap<String, Object>> values,
            TokenMode tokenStrict
    ) throws SkyflowException {
        if (tokens.isEmpty()) {
            LogUtil.printErrorLog(Utils.parameterizedString(
                    ErrorLogs.EMPTY_TOKENS.getLog(), InterfaceName.INSERT.getName()
            ));
            throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyTokens.getMessage());
        }

        for (int index = 0; index < tokens.size(); index++) {
            HashMap<String, Object> tokensMap = tokens.get(index);
            HashMap<String, Object> valuesMap = values.get(index);
            if (tokensMap.size() != valuesMap.size() && tokenStrict == TokenMode.ENABLE_STRICT) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.INSUFFICIENT_TOKENS_PASSED_FOR_TOKEN_MODE_ENABLE_STRICT.getLog(),
                        InterfaceName.INSERT.getName()
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(),
                        ErrorMessage.InsufficientTokensPassedForTokenModeEnableStrict.getMessage()
                );
            }
            validateTokensMapWithTokenStrict(tokensMap, valuesMap, InterfaceName.INSERT.getName());
        }
    }

    private static void validateTokensMapWithTokenStrict(
            HashMap<String, Object> tokensMap, HashMap<String, Object> valuesMap
    ) throws SkyflowException {
        validateTokensMapWithTokenStrict(tokensMap, valuesMap, InterfaceName.UPDATE.getName());
    }

    private static void validateTokensMapWithTokenStrict(
            HashMap<String, Object> tokensMap, HashMap<String, Object> valuesMap, String interfaceName
    ) throws SkyflowException {
        for (String key : tokensMap.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.EMPTY_OR_NULL_KEY_IN_TOKENS.getLog(), interfaceName
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyKeyInTokens.getMessage());
            } else if (!valuesMap.containsKey(key)) {
                LogUtil.printErrorLog(Utils.parameterizedString(
                        ErrorLogs.MISMATCH_OF_FIELDS_AND_TOKENS.getLog(), interfaceName
                ));
                throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.MismatchOfFieldsAndTokens.getMessage());
            } else {
                Object value = tokensMap.get(key);
                if (value == null || value.toString().trim().isEmpty()) {
                    LogUtil.printErrorLog(Utils.parameterizedString(
                            ErrorLogs.EMPTY_OR_NULL_VALUE_IN_TOKENS.getLog(),
                            interfaceName, key
                    ));
                    throw new SkyflowException(ErrorCode.INVALID_INPUT.getCode(), ErrorMessage.EmptyValueInTokens.getMessage());
                }
            }
        }
    }
}
