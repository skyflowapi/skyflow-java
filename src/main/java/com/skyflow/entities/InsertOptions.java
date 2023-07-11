/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * This is the description for InsertOptions Class.
 */
public class InsertOptions {

    private boolean tokens;
    private UpsertOption[] upsertOptions;

    /**
     * @ignore
     */
    public InsertOptions() {
        this.tokens = true;
        this.upsertOptions = null;
    }

    /**
     * @ignore
     */
    public InsertOptions(boolean tokens) {
        this.tokens = tokens;
        this.upsertOptions = null;
    }

    /**
     * @ignore
     */
    public InsertOptions(UpsertOption[] upsertOptions) {
        this.tokens = true;
        this.upsertOptions = upsertOptions;
    }

    /**
     * @param tokens This is the description for tokens parameter.
     * @param upsertOptions This is the description for upsertOptions parameter.
     */
    public InsertOptions(boolean tokens, UpsertOption[] upsertOptions) {
        this.tokens = tokens;
        this.upsertOptions = upsertOptions;
    }

    /**
     * This is the description for isTokens method.
     * @return This is the description of what the method returns.
     */
    public boolean isTokens() {
        return tokens;
    }

    /**
     * This is the description for getUpsertOptions method.
     * @return This is the description of what the method returns.
     */
    public UpsertOption[] getUpsertOptions() {
        return upsertOptions;
    }
}
