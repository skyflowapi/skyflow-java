/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class InsertOptions {

    private boolean tokens;
    private UpsertOption[] upsertOptions;

    public InsertOptions() {
        this.tokens = true;
        this.upsertOptions = null;
    }

    public InsertOptions(boolean tokens) {
        this.tokens = tokens;
        this.upsertOptions = null;
    }

    public InsertOptions(UpsertOption[] upsertOptions) {
        this.tokens = true;
        this.upsertOptions = upsertOptions;
    }

    public InsertOptions(boolean tokens, UpsertOption[] upsertOptions) {
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
