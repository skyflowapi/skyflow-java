package com.skyflow.utils.probe;

import java.util.List;
import java.util.Map;

import com.skyflow.errors.SkyflowException;

/**
 * Builder-backed request for inserting records into a vault.
 */
public class ProbeInsertRequest {

    private String vaultID;
    private String tableName;
    private List<Map<String, Object>> values;
    private ProbeInsertOptions options;

    public static class Builder {
        private final ProbeInsertRequest request = new ProbeInsertRequest();

        public Builder setVaultID(String vaultID) {
            request.vaultID = vaultID;
            return this;
        }

        public Builder setTableName(String tableName) {
            request.tableName = tableName;
            return this;
        }

        public Builder setValues(List<Map<String, Object>> values) {
            request.values = values;
            return this;
        }

        public Builder setOptions(ProbeInsertOptions options) {
            request.options = options;
            return this;
        }

        public ProbeInsertRequest build() throws SkyflowException {
            if (request.vaultID == null || request.vaultID.equals("")) {
                throw new SkyflowException("vaultID is required");
            }
            if (request.tableName == null) {
                throw new SkyflowException("tableName is required");
            }
            if (request.values == null || request.values.size() == 0) {
                throw new SkyflowException("values cannot be empty");
            }
            return request;
        }
    }

    public String getVaultID() {
        return vaultID;
    }

    public String getTableName() {
        return tableName;
    }

    public List<Map<String, Object>> getValues() {
        return values;
    }

    public ProbeInsertOptions getOptions() {
        return options;
    }
}
