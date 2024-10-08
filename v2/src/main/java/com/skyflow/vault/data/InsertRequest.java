package com.skyflow.vault.data;

import com.skyflow.enums.Byot;

import java.util.ArrayList;
import java.util.HashMap;

public class InsertRequest {
    private final String table;
    private final ArrayList<HashMap<String, String>> values;
    private final ArrayList<HashMap<String, String>> tokens;
    private final Boolean returnTokens;
    private final String upsert;
    private final Boolean homogeneous;
    private final Boolean tokenMode;
    private final Byot tokenStrict;

    private InsertRequest(InsertRequestBuilder builder) {
        this.table = builder.table;
        this.values = builder.values;
        this.tokens = builder.tokens;
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

    public ArrayList<HashMap<String, String>> getValues() {
        return this.values;
    }

    public ArrayList<HashMap<String, String>> getTokens() {
        return this.tokens;
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
        private ArrayList<HashMap<String, String>> values;
        private ArrayList<HashMap<String, String>> tokens;
        private Boolean returnTokens = true;
        private String upsert;
        private Boolean homogeneous;
        private Boolean tokenMode;
        private Byot tokenStrict = Byot.DISABLE;

        public InsertRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public InsertRequestBuilder values(ArrayList<HashMap<String, String>> values) {
            this.values = values;
            return this;
        }

        public InsertRequestBuilder tokens(ArrayList<HashMap<String, String>> tokens) {
            this.tokens = tokens;
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
