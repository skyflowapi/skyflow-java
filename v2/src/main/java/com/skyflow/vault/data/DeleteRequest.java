package com.skyflow.vault.data;

import java.util.ArrayList;

public class DeleteRequest {
    private final String table;
    private final ArrayList<String> ids;

    private DeleteRequest(DeleteRequestBuilder builder) {
        this.table = builder.table;
        this.ids = builder.ids;
    }

    public static DeleteRequestBuilder builder() {
        return new DeleteRequestBuilder();
    }

    public String getTable() {
        return table;
    }

    public ArrayList<String> getIds() {
        return ids;
    }

    public static class DeleteRequestBuilder {
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
