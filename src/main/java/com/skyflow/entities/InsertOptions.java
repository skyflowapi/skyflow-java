/*
	Copyright (c) 2022 Skyflow, Inc. 
*/
package com.skyflow.entities;

/**
 * Additional parameters for inserting data.
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
     * @param tokens Whether or not to return tokens for the collected data.
     * @param upsertOptions Upsert configuration for the element.
     */
    public InsertOptions(boolean tokens, UpsertOption[] upsertOptions) {
        this.tokens = tokens;
        this.upsertOptions = upsertOptions;
    }

    /**
     * Checks whether or not to return tokens.
     * @return Returns the value of tokens option.
     */
    public boolean isTokens() {
        return tokens;
    }

    /**
     * Configuration for upserting collected data.
     * @return Returns the value of upsert options.
     */
    public UpsertOption[] getUpsertOptions() {
        return upsertOptions;
    }
}
