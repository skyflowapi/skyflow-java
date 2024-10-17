package com.skyflow.utils.validations;

import com.skyflow.config.ConnectionConfig;
import com.skyflow.config.Credentials;
import com.skyflow.config.VaultConfig;
import com.skyflow.enums.Byot;
import com.skyflow.enums.RedactionType;
import com.skyflow.errors.SkyflowException;
import com.skyflow.utils.ColumnValue;
import com.skyflow.vault.data.*;
import com.skyflow.vault.tokens.DetokenizeRequest;
import com.skyflow.vault.tokens.TokenizeRequest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Validations {

    public static void validateVaultConfig(VaultConfig vaultConfig) throws SkyflowException {
        String vaultId = vaultConfig.getVaultId();
        String clusterId = vaultConfig.getClusterId();
        if (vaultId == null || vaultId.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (clusterId == null || clusterId.trim().isEmpty()) {
            throw new SkyflowException();
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
    }

    public static void validateCredentials(Credentials credentials) throws SkyflowException {
        int nonNullMembers = 0;
        if (credentials.getPath() != null) nonNullMembers++;
        if (credentials.getCredentialsString() != null) nonNullMembers++;
        if (credentials.getToken() != null) nonNullMembers++;

        if (nonNullMembers != 1) {
            throw new SkyflowException();
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
            for (String token : tokens) {
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
        Boolean returnTokens = insertRequest.getReturnTokens();
        String upsert = insertRequest.getUpsert();
        Boolean homogeneous = insertRequest.getHomogeneous();
        Boolean tokenMode = insertRequest.getTokenMode();
        Byot tokenStrict = insertRequest.getTokenStrict();

        if (table == null || table.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (values == null || values.isEmpty()) {
            throw new SkyflowException();
        } else if (upsert != null && upsert.trim().isEmpty()) {
            throw new SkyflowException();
        } else if (homogeneous != null && homogeneous) {
            throw new SkyflowException();
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
            case ENABLE:
                if (tokens == null) {
                    throw new SkyflowException();
                }
            case ENABLE_STRICT:
                if (tokens.size() != values.size()) {
                    throw new SkyflowException();
                }
        }
        validateTokensForInsertRequest(tokens, values);

    }

    public static void validateGetRequest(GetRequest getRequest) throws SkyflowException {
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
        } else if (tokens == null || tokens.isEmpty()) {
            throw new SkyflowException();
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

        if(columnValues == null || columnValues.isEmpty()) {
            throw new SkyflowException();
        } else {
            for(ColumnValue value : columnValues) {
                if (value.getValue() == null || value.getValue().isEmpty() ) {
                    throw new SkyflowException();
                }
                else if(value.getColumnGroup() ==  null || value.getColumnGroup().isEmpty()) {
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
