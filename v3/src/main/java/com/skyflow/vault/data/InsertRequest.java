package com.skyflow.vault.data;

import com.skyflow.enums.UpsertType;

import java.util.ArrayList;
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

    public List<String> getUpsert() {
        return this.builder.upsert;
    }

    public UpsertType getUpsertType() {
        return this.builder.upsertType;
    }

    public ArrayList<InsertRecord> getRecords(){
        return this.builder.records;
    }

    public static final class InsertRequestBuilder extends BaseInsertRequestBuilder {
        private List<String> upsert;

        private UpsertType upsertType;

        private ArrayList<InsertRecord> records;

        private InsertRequestBuilder() {
            super();
        }

        @Override
        public InsertRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        public InsertRequestBuilder upsert(List<String> upsert) {
            this.upsert = upsert;
            return this;
        }

        public InsertRequestBuilder upsertType(UpsertType upsertType) {
            this.upsertType = upsertType;
            return this;
        }

        public InsertRequestBuilder records(ArrayList<InsertRecord> records){
            this.records = records;
            return this;
        }

        public InsertRequest build() {
            return new InsertRequest(this);
        }
    }
}
