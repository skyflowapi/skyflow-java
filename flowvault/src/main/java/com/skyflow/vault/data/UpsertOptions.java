package com.skyflow.vault.data;

import java.util.List;

public class UpsertOptions {
    private final UpsertOptionsBuilder builder;

    private UpsertOptions(UpsertOptionsBuilder builder) {
        this.builder = builder;
    }

    public static UpsertOptionsBuilder builder() {
        return new UpsertOptionsBuilder();
    }

    public String getUpdateType() {
        return this.builder.updateType;
    }

    public List<String> getUniqueColumns() {
        return this.builder.uniqueColumns;
    }

    public static final class UpsertOptionsBuilder {
        private String updateType;
        private List<String> uniqueColumns;

        public UpsertOptionsBuilder updateType(String updateType) {
            this.updateType = updateType;
            return this;
        }

        public UpsertOptionsBuilder uniqueColumns(List<String> uniqueColumns) {
            this.uniqueColumns = uniqueColumns;
            return this;
        }

        public UpsertOptions build() {
            return new UpsertOptions(this);
        }
    }
}
