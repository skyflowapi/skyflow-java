package com.skyflow.entities;

public class InsertBulkOptions {

    private boolean tokens;
    private UpsertOption[] upsertOptions;

    public InsertBulkOptions() {
        this.tokens = true;
        this.upsertOptions = null;
    }

    public InsertBulkOptions(boolean tokens) {
        this.tokens = tokens;
        this.upsertOptions = null;
    }

    public InsertBulkOptions(UpsertOption[] upsertOptions) {
        this.tokens = true;
        this.upsertOptions = upsertOptions;
    }

    public InsertBulkOptions(boolean tokens, UpsertOption[] upsertOptions) {
        this.tokens = tokens;
        this.upsertOptions = upsertOptions;
    }

    public boolean isTokens() {
        return tokens;
    }

    public UpsertOption[] getUpsertOptions() {
        return upsertOptions;
    }
}
