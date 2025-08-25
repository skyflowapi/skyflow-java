package com.skyflow.vault.data;

import com.skyflow.enums.UpdateType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class InsertRequest extends BaseInsertRequest {
    private final InsertRequestBuilder builder;

    private InsertRequest(InsertRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }

    public static InsertRequestBuilder builder() {
        return new InsertRequestBuilder();
    }

    public Boolean getReturnData() {
        return this.builder.returnData;
    }
    public List<String> getUpsert() {
        return this.builder.upsert;
    }
    public UpdateType getUpsertType() {
        return this.builder.upsertType;
    }
//    public Boolean getReturnTokens() {
//        return this.builder.returnTokens;
//    }

    public static final class InsertRequestBuilder extends BaseInsertRequestBuilder {
        private Boolean returnData;
        private List<String> upsert;

        private UpdateType upsertType;

        private InsertRequestBuilder() {
            super();
            this.returnData = false;
        }

        @Override
        public InsertRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        @Override
        public InsertRequestBuilder values(ArrayList<HashMap<String, Object>> values) {
            super.values(values);
            return this;
        }

        public InsertRequestBuilder upsert(List<String> upsert) {
            this.upsert = upsert;
            return this;
        }
        public InsertRequestBuilder upsertType(UpdateType upsertType) {
            this.upsertType = upsertType;
            return this;
        }
//        @Override
//        public InsertRequestBuilder returnTokens(Boolean returnTokens) {
//            super.returnTokens(returnTokens);
//            return this;
//        }

        public InsertRequestBuilder returnData(Boolean returnData) {
            this.returnData = returnData;
            return this;
        }
        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
