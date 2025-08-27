package com.skyflow.vault.data;

import java.util.ArrayList;

public class DeleteRequest {
    private final DeleteRequestBuilder builder;

    private DeleteRequest(DeleteRequestBuilder builder) {
        this.builder = builder;
    }

    public static DeleteRequestBuilder builder() {
        return new DeleteRequestBuilder();
    }

    public String getTable() {
        return this.builder.table;
    }

    public ArrayList<String> getIds() {
        return this.builder.ids;
    }

    public static final class DeleteRequestBuilder {
        private String table;
        private ArrayList<String> ids;

        private DeleteRequestBuilder() {
        }

        public DeleteRequestBuilder table(String table) {
            this.table = table;
            return this;
        }

        public DeleteRequestBuilder ids(ArrayList<String> ids) {
            this.ids = ids;
            return this;
        }

        public DeleteRequest build() {
            return new DeleteRequest(this);
        }
    }
}
