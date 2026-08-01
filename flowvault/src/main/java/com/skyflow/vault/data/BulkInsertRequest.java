package com.skyflow.vault.data;

import com.skyflow.enums.UpsertType;

import java.util.ArrayList;
import java.util.List;

public class BulkInsertRequest extends BaseInsertRequest {
    private final BulkInsertRequestBuilder builder;

    private BulkInsertRequest(BulkInsertRequestBuilder builder) {
        super(builder);
        this.builder = builder;
    }


    public static BulkInsertRequestBuilder builder() {
        return new BulkInsertRequestBuilder();
    }

    public List<String> getUpsert() {
        return this.builder.upsert;
    }

    public UpsertType getUpsertType() {
        return this.builder.upsertType;
    }

    public ArrayList<BulkInsertRecord> getRecords(){
        return this.builder.records;
    }

    public static final class BulkInsertRequestBuilder extends BaseInsertRequestBuilder {
        private List<String> upsert;

        private UpsertType upsertType;

        private ArrayList<BulkInsertRecord> records;

        private BulkInsertRequestBuilder() {
            super();
        }

        @Override
        public BulkInsertRequestBuilder table(String table) {
            super.table(table);
            return this;
        }

        public BulkInsertRequestBuilder upsert(List<String> upsert) {
            this.upsert = upsert;
            return this;
        }

        public BulkInsertRequestBuilder upsertType(UpsertType upsertType) {
            this.upsertType = upsertType;
            return this;
        }

        public BulkInsertRequestBuilder records(ArrayList<BulkInsertRecord> records){
            this.records = records;
            return this;
        }

        public BulkInsertRequest build() {
            return new BulkInsertRequest(this);
        }
    }
}
