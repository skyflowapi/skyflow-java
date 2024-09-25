package com.skyflow.vault.data;

import com.skyflow.enums.Byot;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertRequest {
    private final String table;
    private final ArrayList<HashMap<String, String>> data;
    private final Boolean returnTokens;
    private final String upsert;
    private final Boolean homogeneous;
    private final Boolean tokenMode;
    private final Byot tokenStrict;

    private InsertRequest(InsertRequestBuilder builder) {
        this.table = builder.table;
        this.data = builder.data;
        this.returnTokens = builder.returnTokens;
        this.upsert = builder.upsert;
        this.homogeneous = builder.homogeneous;
        this.tokenMode = builder.tokenMode;
        this.tokenStrict = builder.tokenStrict;
    }

    public static InsertRequestBuilder builder() {
        return new InsertRequestBuilder();
    }

    public String getTable() {
        return table;
    }

    public ArrayList<HashMap<String, String>> getData() {
        return this.data;
    }

    public Boolean getReturnTokens() {
        return returnTokens;
    }

    public String getUpsert() {
        return upsert;
    }

    public Boolean getHomogeneous() {
        return homogeneous;
    }

    public Boolean getTokenMode() {
        return tokenMode;
    }

    public Byot getTokenStrict() {
        return tokenStrict;
    }

    public static final class InsertRequestBuilder {
        private String table;
        private ArrayList<HashMap<String, String>> data;
        private Boolean returnTokens;
        private String upsert;
        private Boolean homogeneous;
        private Boolean tokenMode;
        private Byot tokenStrict;

        public InsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public InsertRequestBuilder data(ArrayList<HashMap<String, String>> data) {
            this.data = data;
            return this;
        }

        public InsertRequestBuilder returnTokens(Boolean returnTokens) {
            this.returnTokens = returnTokens;
            return this;
        }

        public InsertRequestBuilder upsert(String upsert) {
            this.upsert = upsert;
            return this;
        }

        public InsertRequestBuilder homogeneous(Boolean homogeneous) {
            this.homogeneous = homogeneous;
            return this;
        }

        public InsertRequestBuilder tokenMode(Boolean tokenMode) {
            this.tokenMode = tokenMode;
            return this;
        }

        public InsertRequestBuilder tokenStrict(Byot tokenStrict) {
            this.tokenStrict = tokenStrict;
            return this;
        }

        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
