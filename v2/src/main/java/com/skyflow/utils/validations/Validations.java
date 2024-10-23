package com.skyflow.utils.validations;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.Env;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.ColumnValue;
import com.skyflow.utils.Constants;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Validations {

    public static void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        String vaultId = vaultConfig.getVaultId();
        String clusterId = vaultConfig.getClusterId();
        Env env = vaultConfig.getEnv();
        Credentials credentials = vaultConfig.getCredentials();
        if (vaultId == null || vaultId.trim().isEmpty()) {
            if (vaultId == null) {
                // error log for required
            } else {
                // error log for empty
            }
            throw new SkyflowException();
        } else if (clusterId == null || clusterId.trim().isEmpty()) {
            // error log
            throw new SkyflowException();
        } else if (env == null) {
            // error log
            throw new SkyflowException();
        } else if (credentials != null) {
            validateCredentials(credentials);
        }
    }

    public static void validateConnectionConfig(ConnectionConfig connectionConfig) throws SkyflowException {
        String connectionId = connectionConfig.getConnectionId();
        String connectionUrl = connectionConfig.getConnectionUrl();
        if (connectionId == null || connectionId.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (connectionUrl == null || connectionUrl.trim().isEmpty()) {
            throw new SkyflowException();
        }

        try {
            URL url = new URL(connectionUrl);
        } catch (MalformedURLException e) {
            throw new SkyflowException();
        }
    }

    public static void validateCredentials(Credentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        String path = credentials.getPath();
        String credentialsString = credentials.getCredentialsString();
        String token = credentials.getToken();
        String apiKey = credentials.getApiKey();

        if (path != null) nonNullMembers++;
        if (credentialsString != null) nonNullMembers++;
        if (token != null) nonNullMembers++;
        if (apiKey != null) nonNullMembers++;

        if (nonNullMembers != 1) {
            throw new SkyflowException();
        } else if (path != null && path.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (credentialsString != null && credentialsString.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (token != null && token.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (apiKey != null) {
            if (apiKey.trim().isEmpty()) {
                throw new SkyflowException();
            } else {
                Pattern pattern = Pattern.compile(Constants.API_KEY_REGEX);
                Matcher matcher = pattern.matcher(apiKey);
                if (!matcher.matches()) {
                    // invalid api key
                    throw new SkyflowException();
                }
            }
        }
    }

    public static void validateDetokenizeRequest(DetokenizeRequest detokenizeRequest) throws SkyflowException {
        ArrayList<String> tokens = detokenizeRequest.getTokens();
        RedactionType redactionType = detokenizeRequest.getRedactionType();
        Boolean continueOnError = detokenizeRequest.getContinueOnError();
        if (tokens == null || tokens.isEmpty()) {
            throw new SkyflowException();
        } else if (continueOnError == null) {
            throw new SkyflowException();
        } else if (redactionType == null) {
            throw new SkyflowException();
        } else {
            for (int index = 0; index < tokens.size(); index++) {
                String token = tokens.get(index);
                if (token == null || token.trim().isEmpty()) {
                    throw new SkyflowException();
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
        Byot tokenStrict = insertRequest.getTokenStrict();

        if (table == null || table.trim().isEmpty()) {
            if (table == null) {
                // error log for required
            } else {
                // error log for empty
            }
            throw new SkyflowException();
        } else if (values == null || values.isEmpty()) {
            throw new SkyflowException();
        } else if (upsert != null) {
            if (upsert.trim().isEmpty()) {
                throw new SkyflowException();
            } else if (homogeneous != null && homogeneous) {
                throw new SkyflowException();
            }
        }

        for (HashMap<String, Object> valuesMap : values) {
            for (String key : valuesMap.keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    throw new SkyflowException();
                } else {
                    Object value = valuesMap.get(key);
                    if (value == null || value.toString().trim().isEmpty()) {
                        throw new SkyflowException();
                    }
                }
            }
        }

        switch (tokenStrict) {
            case DISABLE:
                if (tokens != null) {
                    throw new SkyflowException();
                }
                break;
            case ENABLE:
                if (tokens == null) {
                    throw new SkyflowException();
                }
                validateTokensForInsertRequest(tokens, values);
                break;
            case ENABLE_STRICT:
                if (tokens == null) {
                    throw new SkyflowException();
                } else if (tokens.size() != values.size()) {
                    throw new SkyflowException();
                }
                validateTokensForInsertRequest(tokens, values);
                break;
        }
    }

    public static void validateGetRequest(GetRequest getRequest) throws SkyflowException {
        String table = getRequest.getTable();
        ArrayList<String> ids = getRequest.getIds();
        RedactionType redactionType = getRequest.getRedactionType();
        Boolean tokenization = getRequest.getReturnTokens();
        List<String> fields = getRequest.getFields();
        String offset = getRequest.getOffset();
        String limit = getRequest.getLimit();
        Boolean downloadURL = getRequest.getDownloadURL();
        String columnName = getRequest.getColumnName();
        ArrayList<String> columnValues = getRequest.getColumnValues();
        String orderBy = getRequest.getOrderBy();

        if (table == null || table.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (ids != null) {
            if (ids.isEmpty()) {
                throw new SkyflowException();
            } else {
                for (int index = 0; index < ids.size(); index++) {
                    String id = ids.get(index);
                    if (id == null || id.trim().isEmpty()) {
                        throw new SkyflowException();
                    }
                }
            }
        } else if (fields != null) {
            if (fields.isEmpty()) {
                throw new SkyflowException();
            } else {
                for (int index = 0; index < fields.size(); index++) {
                    String field = fields.get(index);
                    if (field == null || field.trim().isEmpty()) {
                        throw new SkyflowException();
                    }
                }
            }
        } else if (redactionType == null && (tokenization == null || !tokenization)) {
            // missing redaction
            throw new SkyflowException();
        } else if (tokenization != null && tokenization) {
            if (redactionType != null) {
                // redaction and tokenization does not work together
                throw new SkyflowException();
            } else if (columnName != null || columnValues != null) {
                // tokenization does not work with column name/values
                throw new SkyflowException();
            }
        } else if (offset != null && offset.isEmpty()) {
            throw new SkyflowException();
        } else if (limit != null && limit.isEmpty()) {
            throw new SkyflowException();
        }

        if (ids == null && columnName == null && columnValues == null) {
            // either skyflow ids or column name/values must be passed
            throw new SkyflowException();
        } else if (ids != null && (columnName != null || columnValues != null)) {
            // skyflow ids and column name/values can not be passed together
            throw new SkyflowException();
        } else if (columnName == null && columnValues != null) {
            // column name and values must both be passed
            throw new SkyflowException();
        } else if (columnName != null && columnValues == null) {
            // column name and values must both be passed
            throw new SkyflowException();
        } else if (columnName != null) {
            if (columnValues.isEmpty()) {
                throw new SkyflowException();
            } else {
                for (int index = 0; index < columnValues.size(); index++) {
                    String columnValue = columnValues.get(index);
                    if (columnValue == null || columnValue.trim().isEmpty()) {
                        throw new SkyflowException();
                    }
                }
            }
        }
    }

    public static void validateUpdateRequest(UpdateRequest updateRequest) throws SkyflowException {
        String table = updateRequest.getTable();
        String skyflowId = updateRequest.getId();
        Boolean returnTokens = updateRequest.getReturnTokens();
        HashMap<String, Object> values = updateRequest.getValues();
        HashMap<String, Object> tokens = updateRequest.getTokens();

        if (table == null || table.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (skyflowId == null || skyflowId.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (returnTokens == null) {
            throw new SkyflowException();
        } else if (values == null || values.isEmpty()) {
            throw new SkyflowException();
        } else if (tokens != null) {
            if (tokens.isEmpty()) {
                throw new SkyflowException();
            } else {
                for (String key : tokens.keySet()) {
                    if (key == null || key.trim().isEmpty()) {
                        throw new SkyflowException();
                    } else {
                        Object value = tokens.get(key);
                        if (value == null || value.toString().trim().isEmpty()) {
                            throw new SkyflowException();
                        }
                    }
                }
            }
        }

        for (String key : values.keySet()) {
            if (key == null || key.trim().isEmpty()) {
                throw new SkyflowException();
            } else {
                Object value = values.get(key);
                if (value == null || value.toString().trim().isEmpty()) {
                    throw new SkyflowException();
                }
            }
        }
    }

    public static void validateDeleteRequest(DeleteRequest deleteRequest) throws SkyflowException {
        String table = deleteRequest.getTable();
        ArrayList<String> ids = deleteRequest.getIds();
        if (table == null || table.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (ids == null || ids.isEmpty()) {
            throw new SkyflowException();
        } else {
            for (String id : ids) {
                if (id.trim().isEmpty()) {
                    throw new SkyflowException();
                }
            }
        }
    }

    public static void validateQueryRequest(QueryRequest queryRequest) throws SkyflowException {
        String query = queryRequest.getQuery();
        if (query == null || query.trim().isEmpty()) {
            throw new SkyflowException();
        }
    }

    public static void validateTokenizeRequest(TokenizeRequest tokenizeRequest) throws SkyflowException {
        List<ColumnValue> columnValues = tokenizeRequest.getColumnValues();

        if (columnValues == null || columnValues.isEmpty()) {
            throw new SkyflowException();
        } else {
            for (ColumnValue value : columnValues) {
                if (value.getValue() == null || value.getValue().isEmpty()) {
                    throw new SkyflowException();
                } else if (value.getColumnGroup() == null || value.getColumnGroup().isEmpty()) {
                    throw new SkyflowException();
                }
            }
        }
    }

    private static void validateTokensForInsertRequest(
            ArrayList<HashMap<String, Object>> tokens,
            ArrayList<HashMap<String, Object>> values
    ) throws SkyflowException {
        if (tokens.isEmpty()) {
            throw new SkyflowException();
        }

        for (int index = 0; index < tokens.size(); index++) {
            HashMap<String, Object> tokensMap = tokens.get(index);
            HashMap<String, Object> valuesMap = values.get(index);

            for (String key : tokensMap.keySet()) {
                if (key == null || key.trim().isEmpty()) {
                    throw new SkyflowException();
                } else if (!valuesMap.containsKey(key)) {
                    throw new SkyflowException();
                } else {
                    Object value = tokensMap.get(key);
                    if (value == null || value.toString().trim().isEmpty()) {
                        throw new SkyflowException();
                    }
                }
            }
        }
    }
}
