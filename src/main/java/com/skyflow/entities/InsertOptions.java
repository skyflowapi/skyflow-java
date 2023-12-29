/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

public class InsertOptions {

    private boolean tokens;
    private UpsertOption[] upsertOptions;
    private Boolean continueOnError;

    public InsertOptions() {
        this.tokens = true;
        this.upsertOptions = null;
        this.continueOnError = null;
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

    public InsertOptions(Boolean continueOnError) {
        this.continueOnError = continueOnError;
    }

    public InsertOptions(boolean tokens, Boolean continueOnError) {
        this.tokens = tokens;
        this.continueOnError = continueOnError;
    }

    public InsertOptions(UpsertOption[] upsertOptions, Boolean continueOnError) {
        this.upsertOptions = upsertOptions;
        this.continueOnError = continueOnError;
    }

    public InsertOptions(boolean tokens, UpsertOption[] upsertOptions, Boolean continueOnError) {
        this.tokens = tokens;
        this.upsertOptions = upsertOptions;
        this.continueOnError = continueOnError;
    }

    public boolean isTokens() {
        return tokens;
    }

    public UpsertOption[] getUpsertOptions() {
        return upsertOptions;
    }

    public Boolean getContinueOnError() {
        return continueOnError;
    }
}
